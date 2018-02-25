package Utils

import groovy.inspect.swingui.AstNodeToScriptVisitor
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement


/**
 * Created by jackjia on 6/2/16.
 */
class Helper {
    public static String getSourceFromNode(ClassNode classNode){
        java.io.StringWriter writer = new java.io.StringWriter();
        AstNodeToScriptVisitor visitor = new AstNodeToScriptVisitor(writer);
        visitor.visitClass(classNode); // replace with proper visit****
//        System.out.println(writer.toString());
        return writer.toString();
    }
    // Stack: 2, Locals: 1
    public static void setStatementId(Statement statement, int id){
        Tuple tuple = new Tuple('id',id)
        String text = new JsonBuilder(tuple).toPrettyString()
        statement.setStatementLabel(text)
        /*if(statement.getStatementLabel()==null){
            Map map = new HashMap()
            map.put('id',id)
            String text = new JsonBuilder(map).toPrettyString()
            statement.setStatementLabel(text)
        }
        else{
            Map map = new JsonSlurper().parseText(statement.getStatementLabel())
            map.put('id',id)
            String text = new JsonBuilder(map).toPrettyString()
            statement.setStatementLabel(text)
        }*/
    }
    public static void setStatementPredecessor(Statement statement, int id){
        Tuple tuple = new Tuple('predecessor',id)
        String text = new JsonBuilder(tuple).toPrettyString()
        statement.setStatementLabel(text)
    }
    public static void setStatementTag(Statement statement, String label){
        Tuple tuple = new Tuple('tag',label)
        String text = new JsonBuilder(tuple).toPrettyString()
        statement.setStatementLabel(text)
    }
    public  static  void setStatementParentMethod(Statement statement, String method){
        Tuple tuple = new Tuple('parent',method)
        String text = new JsonBuilder(tuple).toPrettyString()
        statement.setStatementLabel(text)
    }
    public static Integer getStatementId(Statement statement){
        Integer result
        statement.getStatementLabels().each {
            Tuple tuple = new JsonSlurper().parseText(it)
            if(tuple.get(0)=='id'){
                result = tuple.get(1)
            }
        }
        return result
    }
    public static int getStatementPredecessor(Statement statement){
        String result
        statement.getStatementLabels().each {
            Tuple tuple = new JsonSlurper().parseText(it)
            if(tuple.get(0)=='predecessor'){
                result = tuple.get(1)
            }
        }
        return result
    }
    public static String getStatementTag(Statement statement){
        String result
        statement.getStatementLabels().each {
            Tuple tuple = new JsonSlurper().parseText(it)
            if(tuple.get(0)=='tag'){
                result = tuple.get(1)
            }
        }
        return result
    }
    public static String getStatementParentMethod(Statement statement){
        String result
        statement.getStatementLabels().each {
            Tuple tuple = new JsonSlurper().parseText(it)
            if(tuple.get(0)=='parent'){
                result = tuple.get(1)
            }
        }
    }
    def createClass(String qualifiedClassNodeName) {

        new AstBuilder().buildFromSpec {
            classNode(qualifiedClassNodeName, PUBLIC) {
                classNode Object
                interfaces { classNode GroovyObject }
                mixins { }
            }
        }.first()
    }
    public static void labelMethod(MethodNode node){
        if (node.getCode().class == BlockStatement) {
            BlockStatement block = node.getCode().asType(BlockStatement)
            block.getStatements().each {
                stmt->
                    Helper.setStatementParentMethod(stmt,node.getName())
            }
        }
    }
    public static String getUniqueKeyUsingUUID() {
        // Static factory to retrieve a type 4 (pseudo randomly generated) UUID
        String UUID = UUID.randomUUID().toString();
        return UUID;
    }
    public static Object getLabelField(String labels, String field){
        try{
            def jsonSlurper = new JsonSlurper()
            Map label = jsonSlurper.parseText(labels)
            label.getAt(field)
        }
        catch (e){
            return null
        }

    }
}
