package cfg

import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.SwitchStatement

/**
 * Created by jackjia on 6/22/16.
 */
class ICFG {
    List<CFGNode> cfg
    List<CFGNode> IfStatements
    List<CFGNode> MethodCallExpressions
    List<CFGNode> SwithCaseStatements
    /* Temp variables*/
    int counter = 0
    SwitchStatement currentSwitch
    public ICFG(List<CFGNode> nodes){
        cfg = nodes
        IfStatements = new ArrayList<ICFGNode>()
        MethodCallExpressions = new ArrayList<ICFGNode>()
        SwithCaseStatements = new ArrayList<ICFGNode>()
        init()

        //visitPrinter()
        println '--------------------------------------------'
        sinkPrinter()
        println '--------------------------------------------'
        def sinks = getSinks()
        sinks.each {
            sink->
                println getAnnotation(sink)
                println '******'
        }
    }
    private void init(){
        /* Initialize Id*/
        String currentSwitch
        cfg.each {
            node->
                node.setId(++counter)
                if(node.getStatementType()=='IfStatement'){
                    handleIf(node)
                }
                else if(node.getStatementType() == 'CatchStatement'){
                    handleCatch(node)
                }
                else if (node.getStatementType()=='SwitchStatement'){
                    handleSwitchCase(node)
                }
                else if (node.getExpressionType()=='MethodCallExpression'){
                    MethodCallExpression exp = node.getStatement().asType(ExpressionStatement).getExpression().asType(MethodCallExpression)
                    if(exp.getMethodAsString()?.toLowerCase() == 'subscribe'){
                        handleSubscribe(node)
                    }
                    else if(exp.getMethodAsString()?.toLowerCase() == 'schedule'){
                        handleSchedule(node)
                    }
                    else{
                        handleNormalCall(node)
                    }
                }
                else if (node.getExpressionType()=='MapExpression'){
                    handleRestAPI(node)
                }
                else{
                    //println node.getStatementType()+' '+node.getExpressionType()
                }
        }
    }
    private void handleCatch(ICFGNode catchnode){
        CatchStatement stmt = catchnode.getStatement().asType(CatchStatement)
        BlockStatement block = stmt.getCode()
        block.getStatements().each {
            ICFGNode node = getNodeByLineNumber(it.getLineNumber())
            def tuple = new Tuple(catchnode.getId(),'CATCH',stmt.getExceptionType().getName())
            node.addPredecessor(tuple)
        }
    }
    private void handleRestAPI(ICFGNode apinode){
        MapExpression exp = apinode.getStatement().asType(ExpressionStatement).getExpression()
        apinode.setMetaData(exp.getText())
        /***
         * Inter-procedure dependency analysis, not required for now.
        exp.getMapEntryExpressions().each {
            entry->
                String key = entry.keyExpression.text
                if(key=='GET' || key=='POST'){
                    String value = entry.getValueExpression().text
                    cfg.each {
                        node->
                            if(node.getParent().get(0)==value) {
                                Tuple tuple = new Tuple(apinode.getId(), 'RESTAPI', key, value)
                                node.addPredecessor(tuple)
                            }
                    }
                }
        }*/
    }
    private void handleIf(ICFGNode ifnode){
        IfStatement ifstmt = ifnode.getStatement().asType(IfStatement)
        Integer if_id = ifnode.getId()
        def predecessors = ifnode.getPredecessors()
        if(ifstmt.getIfBlock().getClass()!=EmptyStatement){
            ifstmt.getIfBlock().asType(BlockStatement).statements.each {
                ICFGNode node = getNodeByLineNumber(it.getLineNumber())
                def tuple = new Tuple(if_id,'IfStatement','IfBlock',ifstmt.getBooleanExpression().getText())
                node.addPredecessor(tuple)
                //node.clearIndirectPredecessors()
            }
        }
        if(ifstmt.getElseBlock().getClass()==IfStatement){
            IfStatement nestedIf = ifstmt.getElseBlock().asType(IfStatement)
            ICFGNode node = getNodeByLineNumber(nestedIf.getLineNumber())
            def tuple = new Tuple(if_id,'IfStatement','ElseBlock',ifstmt.getBooleanExpression().getText())
            node.addPredecessor(tuple)
            //node.clearIndirectPredecessors()
        }
        else if(ifstmt.getElseBlock().getClass()!=EmptyStatement && ifstmt.getElseBlock().getClass()!=IfStatement){
            if(ifstmt.getElseBlock().getClass()==BlockStatement){
                ifstmt.getElseBlock().asType(BlockStatement).statements.each {
                    ICFGNode node = getNodeByLineNumber(it.getLineNumber())
                    def tuple = new Tuple(if_id,'IfStatement','ElseBlock',ifstmt.getBooleanExpression().getText())
                    node.addPredecessor(tuple)
                    //node.clearIndirectPredecessors()
                }
            }
            else{
                /*
                def statement = ifstmt.getElseBlock()
                ICFGNode node = getNodeByLineNumber(statement.getLineNumber())
                def tuple = new Tuple(if_id,'IfStatement','ElseBlock',ifstmt.getBooleanExpression().getText())
                node.addPredecessor(tuple)
                */
            }
        }
    }
    private void handleSubscribe(ICFGNode methodCallNode){
        MethodCallExpression exp = methodCallNode.getStatement().asType(ExpressionStatement).getExpression().asType(MethodCallExpression)
        methodCallNode.setTag('subscribe')
        methodCallNode.setMetaData(exp.getText())
        /***
         * Inter-procedure dependency analysis, not required for now.
        String triggerName
        String handlerName
        if(exp.getArguments().getAt(2)==null){
            def trigger = exp.getArguments().getAt(0)
            triggerName = trigger.getText()
            def handler = exp.getArguments().getAt(1)
            handlerName = handler.getText()
        }
        else {
            def trigger = exp.getArguments().getAt(1)
            triggerName = trigger.getText()
            def handler = exp.getArguments().getAt(2)
            handlerName = handler.getText()
        }
        Tuple tuple1 = new Tuple(handlerName,1)
        cfg.each {
            node->
                if(node.getParent()==tuple1){
                    def tuple = new Tuple(methodCallNode.getId(),'Subscribe',triggerName,exp.getText())
                    node.addPredecessor(tuple)
                }
        }
         */
    }
    private void handleNormalCall(ICFGNode methodCallNode){
        MethodCallExpression exp = methodCallNode.getStatement().asType(ExpressionStatement).getExpression().asType(MethodCallExpression)
        methodCallNode.setMetaData(exp.getText())
        /***
         * Inter-procedure dependency analysis, not required for now.
        Integer argcount = 0
        def argument = exp.getArguments().getAt(0)
        while(argument!=null){
            argcount++
            argument = exp.getArguments().getAt(argcount)
        }
        Tuple<String,Integer> tuple = new Tuple(exp.getMethodAsString(),argcount)
        cfg.each {
            node->
                // node.getParent()+' '+tuple
                if(node.getParent()==tuple){
                    def tup = new Tuple(methodCallNode.getId(),'MethodCall',exp.getMethodAsString(),exp.getText())
                    node.addPredecessor(tup)
                }
        }
         */
    }
    private void handleSchedule(ICFGNode methodCallNode){
        MethodCallExpression exp = methodCallNode.getStatement().asType(ExpressionStatement).getExpression().asType(MethodCallExpression)
        def predecessors = methodCallNode.getPredecessors()
        methodCallNode.setTag('schedule')
        methodCallNode.setMetaData(exp.getText())
        /***
         * Inter-procedure dependency analysis, not required for now.
        def trigger = exp.getArguments().getAt(0)
        String triggerName = trigger.getText()
        VariableExpression handler = exp.getArguments().getAt(1)
        String handlerName = handler.getText()
        cfg.each {
            node->
                if(node.getParent()==handlerName){
                    def tuple = new Tuple(methodCallNode.getId(),'Schedule',triggerName,exp.getText())
                    node.addPredecessor(tuple)
                }
        }
         ***/
    }
    private void handleSwitchCase(ICFGNode switchnode){
        if(switchnode.getStatementType() == 'SwitchStatement'){
            SwitchStatement stmt = switchnode.getStatement()
            stmt.getCaseStatements().each {
                case_stmt->
                    BlockStatement block = case_stmt.getCode()
                    block.getStatements().each {
                        statement->
                            ICFGNode node = getNodeByLineNumber(statement.getLineNumber())
                            def tuple = new Tuple(switchnode.getId(),'CASE',stmt.getExpression().getText(),case_stmt.getExpression().text)
                            node.addPredecessor(tuple)
                    }
            }
        }
    }
    public ICFGNode getNodeByLineNumber(Integer number){
        ICFGNode result
        cfg.each {
            node->
                if(node.getLineNumber()==number){
                    result = node
                }
        }
        return result
    }
    public ICFGNode getNodeById(Integer id){
        ICFGNode result
        cfg.each {
            node->
                if(node.getId()==id){
                    result = node
                }
        }
        result
    }
    public ICFGNode get(Integer id){
        ICFGNode result
        cfg.each {
            it->
                if(it.getId()==id){
                    result = it
                }
        }
        result
    }
    public String getAnnotation(Integer id){
        ICFGNode node = getNodeById(id)
        def stack = [] as Stack
        def tuple = new Tuple(id,node.getParent(),'sink',node.getMetaData())
        stack.push(tuple)
        if(node!=null){
            node.getPredecessors().each {
                it->
                    stack.push(it)
                    processPredecessor(it.get(0),stack)
            }
        }
        stack.toString()
    }
    private Stack processPredecessor(Integer id, Stack stack){
        ICFGNode node = getNodeById(id)
        if(node!=null){
            node.getPredecessors().each {
                stack.push(it)
                processPredecessor(it.get(0),stack)
            }
        }
        stack
    }
    public void printTriggers(Integer id){
        ICFGNode node = getNodeById(id)
        Tuple tuple = new Tuple(node.getId(),'sink', node.getLineNumber().toString(),node.getMetaData())
        if(node!=null){
            node.getPredecessors().each {
                it->
                    def q = [] as Queue
                    q.add(tuple)
                    q.add(it)
                    processTriggers(it.get(0),q)
            }
        }
    }
    private void processTriggers(Integer id, Queue queue){
        ICFGNode node = getNodeById(id)
        if(node!=null && node.getPredecessors().size()>0){
            node.getPredecessors().each{
                def q = queue.clone()
                q.add(it)
                processTriggers(it.get(0),q)
            }
        }
        else{
            println queue
        }
    }
    public void visitPrinter(){
        cfg.each {
            node->
                println node.getLineNumber()+'\t'+node.getStatementType()+'\t'+node.getMetaData()+'\t'+node.getId()+'\t'+node.getTag()+'\t'+node.getParent()+'\t'+node.getPredecessors().sort()
        }
    }
    public List<Integer> getSinks(){
        List<Integer> result = new ArrayList<Integer>()
        cfg.each {
            node->
                if(node.getTag()=='sink'){
                    result.add(node.getId())
                }
        }
        result
    }
    public void sinkPrinter(){
        cfg.each {
            node->
                if(node.getTag()=='sink') {
                    println node.getLineNumber() + '\t'+node.getMetaData()+ '\t' + node.getId() + '\t' + node.getTag() + '\t' + node.getParent() + '\t' + node.getPredecessors().sort()
                }
        }
    }
    /*public List<CFGNode> getTriggerNodes(Integer id){
        List<Integer> triggers =
    }*/
}
