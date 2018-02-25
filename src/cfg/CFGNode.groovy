package cfg

import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement

/**
 * Created by jackjia on 6/12/16.
 */
public class CFGNode {
    private Statement statement
    private String stmtType
    private String expType
    private Integer lineNumber
    private Integer id
    private String tag
    private String metadata
    //private Integer predecessor
    private Tuple parent
    private Set<Tuple> predecessors
    public CFGNode(Statement stmt){
        statement = stmt
        lineNumber = stmt.getLineNumber()
        stmtType = statement.getClass().getSimpleName()
        if (statement.getClass()==ExpressionStatement) {
            expType = statement.asType(ExpressionStatement).getExpression().getClass().getSimpleName()
        }
        predecessors = new HashSet<Tuple<Integer,String,String,String>>()
    }
    public void setId (Integer the_id){
        id = the_id
    }
    public void setTag (String the_tag){
        tag = the_tag
    }
    /*public void setPredecessor(Integer id){
        predecessor = id
    }*/
    public void setParent(Tuple parentMethod){
        parent = parentMethod
    }
    public Integer getId(){
        id
    }
    public String getTag(){
        tag
    }
    /*public Integer getPredecessor(){
        predecessor
    }*/
    public Tuple getParent(){
        parent
    }
    public Statement getStatement(){
        statement
    }
    public String getStatementType(){
        stmtType
    }
    public String getExpressionType(){
        expType
    }
    public Integer getLineNumber(){
        lineNumber
    }
    public void addPredecessor(Tuple pred){
        predecessors.add(pred)
    }
    public Set<Tuple> getPredecessors(){
        predecessors
    }
    public String getMetaData(){
        metadata
    }
    public void setMetaData(String data){
        metadata = data
    }
    public void joinPredecessors(Set<Tuple> preds){
        predecessors.addAll(preds)
    }
    public void clearIndirectPredecessors(){
        predecessors.removeAll {
            it->
                it.get(1)!='IfStatement'
        }
    }
}
