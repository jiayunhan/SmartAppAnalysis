package cfg

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.Statement

/**
 * Created by jackjia on 6/3/16.
 */
class BasicBlock {
    private int id;
    private Statement FirstStmt;
    private Statement LastStmt;
    private List<Statement> StmtList
    private MethodNode parent
    private List<BasicBlock> successors
    private List<BasicBlock> predecessors

    public MethodNode getParent() {
        return parent
    }
    public void setParent(MethodNode methodNode){
        parent = methodNode
    }
    public void addSuccessor(BasicBlock bb){
        successors.add(bb)
    }
    public void addPredecessor(BasicBlock bb){
        predecessors.add(bb)
    }

    public List<BasicBlock> getSuccessors(){
        return  successors
    }
    public List<BasicBlock> getPredecessors(){
        return predecessors
    }
    public boolean isSuccessor(BasicBlock bb){
        def id_array = successors.id
        if(bb.id in id_array){
            return true
        }
        else{
            return false
        }
    }
    public boolean isPredecessor(BasicBlock bb){
        def id_array = predecessors.id
        if(bb.id in id_array){
            return true
        }
        else{
            return false
        }
    }
}
