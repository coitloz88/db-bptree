public class BPlusTree {
    /**
     * degree: max degree(m-way)
     *
     * insert: 넣는거 시도하는데 성공적으로 넣어지면 1, 안되면 0을 반환하는 기본 함수로 일단 넣음 나머지 쪼개는거는 비쁠트리 차원에서
     */

    private final int degree;
    private final int totalNumberOfKeys;
    private Node root;

    public BPlusTree(int degree){
        this.degree = degree;
        this.totalNumberOfKeys = degree - 1;
        root = new Node(totalNumberOfKeys, true, null);
    }

    public Node singleKeySearchNode(int target, boolean showNodeKey){
        Node tmpNode = root;
        while (!tmpNode.isLeaf()) {
            if (showNodeKey) tmpNode.showKeys();

            // 현재 노드가 가진 key 값에 target 값이 있는지 찾아본다
            int i = tmpNode.findIndexOfKeyInKeyArray(target);
            if(i < tmpNode.getCurrentNumberOfKeys() && target == tmpNode.getKey(i)) tmpNode = tmpNode.getChildNode(i + 1);
            else tmpNode = tmpNode.getChildNode(i);

        }
        return tmpNode;
    }
    
    public String singleKeySearch(int target){
        /**
         * root부터 탐색 시작
         * 동일한 값이 있을 때 return
         * 사이값일 경우 그 사이 노드로 들어감
         *
         * 찾으면 해당하는 value 반환
         */
        if(root.getCurrentNumberOfKeys() == 0) return "root is empty";
        Node tmpNode = singleKeySearchNode(target, true);

        //leaf 도달
        int i = tmpNode.findIndexOfKeyInKeyArray(target);

        if (i < tmpNode.getCurrentNumberOfKeys() && target == tmpNode.getKey(i))
            return String.valueOf(tmpNode.getValue(i));
        else return "NOT FOUND";
    }

    public String rangeSearch(int startTarget, int endTarget) {
        Node startNode = singleKeySearchNode(startTarget, false);
        Node endNode = singleKeySearchNode(endTarget, false);
        int startNode_i = startNode.findIndexOfKeyInKeyArray(startTarget);

        if (startNode_i >= startNode.getCurrentNumberOfKeys()) {
            if (startNode == endNode) return "NOT FOUND";
            else {
                startNode = startNode.getChildNode(0);
                startNode_i = 0;
            }
        } else if(startNode.getKey(startNode_i) > endTarget){
            return "NOT FOUND";
        }

        while (startNode != endNode) {
            for (int i = startNode_i; i < startNode.getCurrentNumberOfKeys(); i++) {
                System.out.println(startNode.getKey(i) + "," + startNode.getValue(i));
            }
            startNode_i = 0;
            startNode = startNode.getChildNode(0);
        }

        for (int i = startNode_i; i < startNode.getCurrentNumberOfKeys() && startNode.getKey(i) <= endTarget; i++) {
            System.out.println(startNode.getKey(i) + "," + startNode.getValue(i));
        }
        return "";
    }

    public void insert(int inputKey, int inputValue){
        //중복 key는 들어오지 않는다
        //TODO: delete로 싹다 지워서 root가 빈 경우(예외처리)?
        Node targetNode = singleKeySearchNode(inputKey, false); //해당하는 key가 들어갈 leaf Node를 찾아준다

        if(targetNode.getCurrentNumberOfKeys() < totalNumberOfKeys){
            targetNode.push(inputKey, inputValue);
        } else {
            //split
            int split_i = degree % 2 == 0 ? degree / 2 : (int) Math.ceil((double)degree / 2) - 1; //longTmpNode의 [split_i]번째를 올림

            /**
             * 1. (totalNumberOfKeys + 1)의 Node를 하나 생성, targetNode의 '값'만 복사해줌 (주소 X) ->virtualNode
             *      leftNode랑 .. 기타등등
             * 2. inputKey, inputValue 쌍을 넣어줌 push_back해줌
             * 3. rightChildNode(new로 동적할당) -> longTmpNode의 [split_i] 이후의 값을 대입
             * 4. targetNode의 rightNode가 rightChildNode를 가리키게 함 #targetNode가 root인지 확인!!(5와 예외처리)
             * 5. virutalNode의 keys[split_i]를 parent node에 internalNodeInsert()함
             */

            Node virtualNode = new Node(totalNumberOfKeys + 1, true, null);

            //복사 및 split
            for (int i = 0; i < totalNumberOfKeys; i++) {
                virtualNode.push(targetNode.getKey(i), targetNode.getValue(i));
                targetNode.setKey(0, i); targetNode.setValue(0, i);
            }
            virtualNode.push(inputKey, inputValue); //하나 남는 자리에 inputKey와 inputValue 넣어줌
            virtualNode.setChildNode(targetNode.getChildNode(0) , 0);
            targetNode.setCurrentNumberOfKeys(0);

            for (int i = 0; i < split_i; i++) {
                targetNode.push(virtualNode.getKey(i), virtualNode.getValue(i));
            }

            Node rightNode = new Node(totalNumberOfKeys, true, targetNode.getParent());

            for (int i = split_i; i < totalNumberOfKeys + 1; i++) {
                rightNode.push(virtualNode.getKey(i), virtualNode.getValue(i));
            }
            rightNode.setChildNode(virtualNode.getChildNode(0), 0);
            targetNode.setChildNode(rightNode, 0);
            //분할 완료

            //parent Node로 올려주기
            if(targetNode == root){
                //TODO: root인 경우 새로운 root를 파고 그 root의 좌우를 targetNode, rightNode로 설정해주고 각 노드의 부모를 새로운 root로 한다
                Node newRoot = new Node(totalNumberOfKeys, false, null);
                newRoot.push(virtualNode.getKey(split_i), 0);

                newRoot.setChildNode(targetNode, 0); newRoot.setChildNode(rightNode, 1);
                targetNode.setParent(newRoot); rightNode.setParent(newRoot);
                root = newRoot;
               } else {
                internalNodeInsert(targetNode.getParent(), rightNode, virtualNode.getKey(split_i));
            }
        }
    }

    //leafNode 위의 Node도 다차서 parent node를 또 쪼개야할때 재귀적으로 호출하는 함수
    public void internalNodeInsert(Node parentNode, Node childNode, int inputKey){
        if(parentNode.getCurrentNumberOfKeys() < totalNumberOfKeys){
            parentNode.push(inputKey, 0);

            int num = parentNode.findIndexOfKeyInKeyArray(inputKey);
            num = num >= parentNode.getCurrentNumberOfKeys() ? num - 1: num;
            parentNode.setChildNode(childNode, num + 1); //TODO:순서 맞는지 (제정신일때) 확인!

            return;
        } else {
            //parent도 자리가 없다.. 쪼개줌
            int split_i = degree % 2 == 0 ? degree / 2 : (int) Math.ceil((double)degree / 2) - 1;

            //virtualNode에 기존 parentNode의 값 복사
            Node virtualNode = new Node(totalNumberOfKeys + 1, false, null);
            for (int i = 0; i < totalNumberOfKeys; i++) {
                    virtualNode.setKey(parentNode.getKey(i), i);
                    virtualNode.setChildNode(parentNode.getChildNode(i), i);
                    parentNode.setKey(0,i); parentNode.setChildNode(null, i); //삽입한 parentNode는 초기화 해준다.
            } virtualNode.setChildNode(parentNode.getChildNode(totalNumberOfKeys), totalNumberOfKeys);
            virtualNode.setCurrentNumberOfKeys(parentNode.getCurrentNumberOfKeys());
            virtualNode.push(inputKey, 0);
            int num = virtualNode.findIndexOfKeyInKeyArray(inputKey);
            num = num >= virtualNode.getCurrentNumberOfKeys() ? num - 1: num;

            virtualNode.setChildNode(childNode, num + 1); //넣어야할 곳(push_back에서 자리를 만들어줌)에 삽입해준다.
            parentNode.setCurrentNumberOfKeys(0);

            Node rightNode = new Node(totalNumberOfKeys, false, parentNode.getParent()); //parent노드 오른쪽으로 쪼개질 노드(parent의 parent에 붙음)
            for (int i = 0; i < split_i; i++) {
                parentNode.setKey(virtualNode.getKey(i), i);
                parentNode.setChildNode(virtualNode.getChildNode(i), i);
            } parentNode.setChildNode(virtualNode.getChildNode(split_i), split_i);
            parentNode.setCurrentNumberOfKeys(split_i);

            for (int i = split_i + 1; i < totalNumberOfKeys + 1; i++) {
                //rightNode에 virtualNode[split_i + 1] 부터 key랑 node 복사(leaf가 아님!)
                //parentNode의 [split_i]부터 key랑 node 초기화
                rightNode.setKey(virtualNode.getKey(i), i - split_i - 1);
                rightNode.setChildNode(virtualNode.getChildNode(i) , i - split_i - 1);
            } rightNode.setChildNode(virtualNode.getChildNode(totalNumberOfKeys + 1), totalNumberOfKeys - split_i);
            rightNode.setCurrentNumberOfKeys(totalNumberOfKeys - split_i);

            for (int i = 0; i <= rightNode.getCurrentNumberOfKeys(); i++) {
                if(rightNode.getChildNode(i) != null) rightNode.getChildNode(i).setParent(rightNode);
            }
            //parent Node로 올려주기
            if(parentNode == root){
                //TODO: root인 경우 새로운 root를 파고 그 root의 좌우를 parentNode, rightNode로 설정해주고 각 노드의 부모를 새로운 root로 한다
                Node newRoot = new Node(totalNumberOfKeys, false, null);
                newRoot.push(virtualNode.getKey(split_i), 0);

                newRoot.setChildNode(parentNode, 0); newRoot.setChildNode(rightNode, 1);
                parentNode.setParent(newRoot); rightNode.setParent(newRoot);
                root = newRoot;
                return;
            } else {
                internalNodeInsert(parentNode.getParent(), rightNode, virtualNode.getKey(split_i));
            }
        }

    }

    public void delete(int deleteKeyTarget){
        //TODO: delete 함수 구현

        /**
         * - 최소 (ceil(degree / 2) - 1)개의 key를 가지고 있어야 한다.
         */

        Node searchLeafNode = singleKeySearchNode(deleteKeyTarget, false);
        /*TODO: search할때 internal Node에 해당 key값이 있는지 같이 찾아야할듯
        ``근데 위에 투두.. 생각해보니까 leaf node이면서 [0]번째 key는 항상 internal에 존재하는듯? key가 최소일때 빼고...!*/
        /**
         * 1. 서치리프노드의 key개수가 min보다 클때 - 단순삭제 .. 인듯 하지만
         *  1-1. 서치리프노드의 [0]번째 key가 아닌경우 -> 단순삭제
         *  1-2. 서치리프노드의 [0]번째 key인 경우 -> internal node 존재, 찾아서 삭제(유일하게 key가 최소이면서 [0]인 경우 internal에 없음! 그래서 search를 하긴해야함)
         *      1-2-1. internal node에서 merge가 일어나는 경우...? 있으니까 조심해라.
         *
         * 2. 서치리프노드의 key개수가 min보다 같거나 작을때 -> 왼쪽오른쪽(갈수있는지부터 확인) 옆노드에서 빌려올 수 있는지 확인
         *  2-1. 빌려올 수 있는 경우: 빌려와서 재구성
         *  2-2. 빌려올 수 없는 경우: recursive하게 merge
         */

        int target_i = searchLeafNode.findIndexOfKeyInKeyArray(deleteKeyTarget);

        if (deleteKeyTarget != searchLeafNode.getKey(target_i) || target_i >= searchLeafNode.getCurrentNumberOfKeys()){
            System.out.println("There is no " + deleteKeyTarget + " in the b+tree.");
            return; //삭제하려는 노드가 없는 경우
        }

        int minNumberOfKeys = (int) Math.ceil((double)degree / 2) - 1; //leaf node는 최소 이 변수만큼 key를 가져야 한다.

        if(searchLeafNode.getCurrentNumberOfKeys() > minNumberOfKeys){
            //노드에 key가 많은 경우
            if(deleteKeyTarget == searchLeafNode.getKey(0)){
                //internal node에 있는 값도 삭제해줘야함! 삭제된 곳은 leaf node에 남은 애로 채워줌.

            } else {
                //TODO: 단순 삭제 구현
                searchLeafNode.pull(deleteKeyTarget);
            }

        } else {
            //노드에 key가 적은 경우
        }

    }
/*
    public void showAllLeafKeys(){
        Node tmpNode = singleKeySearchNode(0, false);
        while(tmpNode != null){
            tmpNode.showKeys();
            tmpNode = tmpNode.getChildNode(0);
        }
    }
*/

}
