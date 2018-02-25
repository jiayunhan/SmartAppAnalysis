/**
 * Created by jackjia on 5/1/16.
 */


import Utils.Builder
import Utils.CountVisitor
import Utils.Helper
import Utils.InitVisitor
import Utils.PatchVisitor
import cfg.CFG
import cfg.ICFG
import cfg.ICFGNode
import cfg.CFGNode
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.CompilationCustomizer
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.ExpressionStatement

import javax.lang.model.element.VariableElement

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class STAnalysisAST extends CompilationCustomizer{

    static final VALUE = 'value'
    static final DOLLAR = '$'
    static final PUBLIC = ClassNode.ACC_PUBLIC

    Logger log
    Map allCommands
    Map allProps

    List allCommandsList
    List allPropsList
    List allCapsList

    List<ICFGNode> nodes
    ICFG cfg
    String appId
    String appName
    String appDescription
    String appCategory
    List<String> validMethods
    public STAnalysisAST(Logger logger){
        super(CompilePhase.SEMANTIC_ANALYSIS)
        log = logger
        allCommands = new HashMap()
        allProps = new HashMap()
        allCommandsList = new ArrayList()
        allPropsList = new ArrayList()
        allCapsList = new ArrayList()
        validMethods = new ArrayList<String>()
        nodes = new ArrayList<ICFGNode>()
    }
    Integer sink_count
    @Override
    void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
//        source.getAST().getMethods().each {
//            it ->
//                if(it.getName() == 'installed'){
//                    List existingStatements = it.getCode().getStatements()
//                    existingStatements.add(Builder.buildAssignmentStatement('data','haha'))
//                }
//                //def statement = it.get
//                //print statement.getLineNumber()+"\n"
//        }
        /*CountVisitor countVisitor = new CountVisitor()
        classNode.visitContents(countVisitor)*/


        println "------------Start Dynamic Analysis--------------"

        sink_count = 0
        ConstructVisitor cv = new ConstructVisitor()
        InitVisitor inv = new InitVisitor(allCommandsList)
        classNode.visitContents(cv)
        println sink_count


        classNode.visitContents(inv)
        cfg = new ICFG(nodes)
        AnnotationVisitor anv = new AnnotationVisitor(cfg)
        classNode.visitContents(anv)

        println "=============before patch============="

        println Helper.getSourceFromNode(classNode)

        Patch(source,classNode)


//        nodes.each {
//            def pred = it.predecessors
//            if (!pred.empty) {
//                println "######### predecessor set"
//                pred.each {
//                    println "######{$it[0]}"
//                }
//            }
//        }

        println "=============after patch============="

        // change the code to groovy syntax

        def code = Helper.getSourceFromNode(classNode)

        code = code.replaceAll(/public java.lang.Object/, "def")

        code = code.substring(code.indexOf('def run'))

        def pre_code = code.substring(0,code.indexOf('def installed'))

        def definition = pre_code.substring(pre_code.indexOf('definition'),pre_code.indexOf('this.preferences'))

        def preferences = pre_code.substring(pre_code.indexOf('preferences'),pre_code.indexOf('this.mappings'))

        def mappings = pre_code.substring(code.indexOf('mappings'), pre_code.length()-2)

        // parseing definition
        def def_names = []
        def def_values = []
        def def_arr = definition.substring(definition.indexOf('([')+2, definition.indexOf('])'))
        while (1) {
            // what if there is ' in string
            def_names << def_arr.substring(1, def_arr.indexOf(':')-1)
            if (def_arr.indexOf("', ") != -1) {
                def_values << def_arr.substring(def_arr.indexOf(': ')+3, def_arr.indexOf("', "))
                def_arr = def_arr.substring(def_arr.indexOf("', ")+3)
            }
            else {
                //at the end of the array
                def_values << def_arr.substring(def_arr.indexOf(': ')+3, def_arr.length()-1)
                break
            }
        }

        def new_definition = "definition(\n"

        for (int i = 0; i < def_names.size(); i++) {
            new_definition = new_definition + '\t' + def_names[i] + ':' + '"' + def_values[i] + '",\n'
        }

        new_definition = new_definition + '\toauth: true)\n'

//        println new_definition


        /*
        We need to parse out the value of specific devices they registered to
         */
        // parse preference
        def new_preferences = 'preferences {\n'

        def device_name_arr = []
        def input_arr = []
        def sections = preferences.substring(preferences.indexOf('this.section'), preferences.length()-3)

        while (1) {
            new_preferences = new_preferences+'\tsection('+sections.substring(sections.indexOf('section(')+8, sections.indexOf("',")+1)+'){\n'
            // input array
            sections = sections.substring(sections.indexOf('section(')+8)
            def section = sections
            if (sections.indexOf('section') != -1) {
                section = section.substring(0, sections.indexOf('section') )
            }
            while (1) {
                if (section.indexOf('input') == -1) break
                else {
                    def input = section.substring(section.indexOf('input')+6, section.indexOf(')\n'))

                    input = input.replaceAll(/[\[\]\(\)]/, "")
                    input = input.replaceAll(/\'[^\']+\'\:/) {m -> "${m.substring(1,m.length()-2)}:"}
                    if (input.indexOf("description") == -1 && input.indexOf("title") == -1) {
                        input_arr << input.substring(input.indexOf("'") + 1).substring(0, input.substring(input.indexOf("'") + 1).indexOf("'"))
                    }
                    new_preferences = new_preferences + '\tinput ' + input + '\n'
                    section = section.substring(sections.indexOf('input')+5)
                }
            }
            new_preferences = new_preferences + '\t}\n'
            if (sections.indexOf('section') == -1) break
            sections = sections.substring(sections.indexOf('section('))
        }

        new_preferences = new_preferences + '}\n'

//        println new_preferences

        def new_mappings = "mappings {\n"
        def paths = mappings.substring(mappings.indexOf('path'))

        while(1) {
            def action = paths.substring(paths.indexOf("['")+2, paths.indexOf(":")-1)
            paths = paths.substring(paths.indexOf(":")+2)
            def func = paths.substring(0,paths.indexOf(']'))
            new_mappings = new_mappings + "path {\n\taction: [\n\t\t" + action + ": " + func + "\n\t]\n}\n"
            if (paths.indexOf('path') == -1) break
            paths = paths.substring(paths.indexOf('path'))
        }

        new_mappings = new_mappings + "}\n"

//        println new_mappings

        def post_code = code.substring(code.indexOf('def installed'))

        // fix this syntax
        post_code = post_code.replaceAll(/this./,"")
        // fix object property
        post_code = post_code.replaceAll(/ \./,".")
        // remove java syntax
        post_code = post_code.replaceAll(/java.lang.Object /,"def ")

//        println post_code



        def sendRequest = """
def sendRequest(url,data){
   try{
       httpPost("http://141.212.110.244:80/stbackend/feedback.php",data){
       resp ->
       def cmd = "\${resp.data}"
       def thing = cmd.tokenize('.')[0]
       def action = cmd.tokenize('.')[1]
       log.debug "response data: \${thing}.\${action}()"
       switch (thing) {
"""
        input_arr.each {
            sendRequest = sendRequest + """
            case "$it":
            $it."\$action"()
            break
        """
        }

        sendRequest = sendRequest + """
            default:
            break
        }
        }
   }
   }catch (e){
       log.error "something went wrong: \$e"
       return false
   }
return true
}

"""
        def patched_code = new_definition + new_preferences + new_mappings + post_code + sendRequest

        println patched_code
    }

    private void PatchAST(List statements) {
        def added_stmts = []
        def hint_list = []
        statements.eachWithIndex { it, index ->
//            println it.getClass()
            if(it.getClass()==IfStatement) {
                IfStatement ifstmt = it.asType(IfStatement)
//                println ifstmt.ifBlock
                PatchAST(ifstmt.ifBlock.statements)
                added_stmts << 'context'
                hint_list << ''
                added_stmts << 'control'
                hint_list << ifstmt.getBooleanExpression().getExpression().getText()
                added_stmts << ''
                hint_list << ''
            }
            else if(it.getClass()==ExpressionStatement) {

                ExpressionStatement expstmt = it.asType(ExpressionStatement)
                def stmtlabel_str = expstmt.statementLabel
                def jsonSlurper = new JsonSlurper()
                def stmtlabel_map = jsonSlurper.parseText(stmtlabel_str)
//                    println stmtlabel_map
                if (expstmt.getExpression().getClass() == MethodCallExpression) {
                    //println "Call method: " + stmtlabel_map['metadata']
                    added_stmts << 'context'
                    hint_list << ''
                    added_stmts << 'control'
                    hint_list << stmtlabel_map['metadata']
                    def exp =expstmt.getExpression().asType(MethodCallExpression)
                    if (exp.getMethodAsString()?.toLowerCase() in allCommandsList) {
                        added_stmts << 'sink1'
                        hint_list << ''
                        added_stmts << 'sink2'
                        hint_list << ''
                    }
                    added_stmts << ''
                    hint_list << ''
                }
                else if (expstmt.getExpression().getClass() == DeclarationExpression) {
                    DeclarationExpression dclexpr = expstmt.getExpression().asType(DeclarationExpression)
//                    println "Assignment: " + dclexpr.leftExpression['variable']
                    added_stmts << ''
                    hint_list << ''
                    added_stmts << 'var'
                    hint_list << dclexpr.leftExpression['variable']
                }
                else if (expstmt.getExpression().getClass() == BinaryExpression) {
                    BinaryExpression binexpr = expstmt.getExpression().asType(BinaryExpression)
                    if (binexpr.leftExpression.getClass() == VariableExpression) {
//                        println "Assignment: " + binexpr.leftExpression['variable']
                        added_stmts << ''
                        hint_list << ''
                        added_stmts << 'var'
                        hint_list << binexpr.leftExpression['variable']
                    }
                    else if (binexpr.leftExpression.getClass() == BinaryExpression) {
                        //map
                        added_stmts << ''
                        hint_list << ''
                        added_stmts << 'var'
                        hint_list << binexpr.leftExpression.leftExpression['variable']
                    }
                    else {
                        println expstmt
                        added_stmts << ''
                        hint_list << ''
                    }
                }
                else {
                    println expstmt
                    added_stmts << ''
                    hint_list << ''
                }
            }
        }
        added_stmts.eachWithIndex{ type, index ->
            if (type != ''){
                if (type == 'context') {
                    statements.add(index, Builder.add2contextList('contextList', 'controlList'))
                }
                if (type == 'control') {
                    statements.add(index, Builder.add2controlList('controlList', hint_list[index]))
                }
                if (type == 'var') {
                    statements.add(index, Builder.add2variableList(hint_list[index], hint_list[index]))
                }
                if (type == 'sink1') {
                    statements.add(index, Builder.buildAssignment2State('history','"${state.controlList};${state.contextList}"'))
                }
                if (type == 'sink2') {
                    statements.add(index, Builder.buildSendRequestStatement())
                }
            }
        }
    }

    private void Patch(SourceUnit source,ClassNode classNode){
        appId = Helper.getUniqueKeyUsingUUID()
        classNode.getMethods().each {
            method->
                if(method.getName() == 'run'){
                    List existingStatements = method.getCode().getStatements()
                    existingStatements.add(Builder.buildCallbackMappings('onPermissionResponse'))
                }
                else if(method.getName() == 'installed'){
                    List existingStatements = method.getCode().getStatements()
                    existingStatements.add(Builder.buildAssignment2State('appId',appId))
                    existingStatements.add(Builder.buildAssignmentList2State('actionQueue',[]))
                    existingStatements.add(Builder.buildAssignment2State('appName',appName))
                    existingStatements.add(Builder.buildAssignment2State('appDescription',appDescription))
                    existingStatements.add(Builder.buildAssignment2State('appCategory',appCategory))
                    existingStatements.add(Builder.buildAssignmentList2State('contextList',[]))
                    existingStatements.add(Builder.buildAssignmentList2State('controlList',[]))
                    existingStatements.add(Builder.buildAssignmentMap2State('variableList',[:]))
                    existingStatements.add(Builder.buildAssignment2State('controlHistory',''))
                    existingStatements.add(Builder.buildAssignment2State('contextHistory',''))
                    /* Add variables to log predecessor for each function*/
                    validMethods.each {
                        existingStatements.add(Builder.buildAssignment2State("$it"+"_predecessor",""))
                    }
                }
                else if (method.getName() != 'main' && method.getName() != 'updated'){
                    //println "Method: "+ method.getName()
                    List existingStatements = method.getCode().getStatements()
                    PatchAST(existingStatements)
                }

        }
        PatchVisitor pv = new PatchVisitor(validMethods)
        classNode.visitContents(pv)
    }
    class ConstructVisitor extends ClassCodeVisitorSupport{
        public ArrayList<String> globals
        public ArrayList<DeclarationExpression> dexpressions
        public ArrayList<BinaryExpression> bexpressions
        private Tuple currentMethod
        //private Tuple<String,List<String>> currentMethod
        public ConstructVisitor() {
        }
        @Override
        protected SourceUnit getSourceUnit() {
            return null;
        }
        @Override
        void  visitMethod(MethodNode node){
            currentMethod = new Tuple(node.name,node.getParameters().size())
            if(node.getName() != 'main' && node.getName() != 'run'){
                validMethods.add(node.getName())
            }
            super.visitMethod(node)
        }
        @Override
        void visitStatement(Statement statement) {
            if (statement.getClass() != BlockStatement && statement.getLineNumber()>0) {
                ICFGNode node = new ICFGNode(statement)
                node.setParent(currentMethod)
                if(node.getExpressionType()=='MethodCallExpression'){
                    MethodCallExpression exp = node.getStatement().asType(ExpressionStatement).getExpression().asType(MethodCallExpression)

//                    println "MethodCall: ${exp.getMethodAsString()}"

                    if(exp.getMethod()!=null){
                        if(exp.getMethod().getClass()==GStringExpression){
                            node.setTag('sink')
                            sink_count = sink_count+1
                            node.setMetaData(exp.text)
                        }
                    }
                    if(exp.getMethodAsString()?.toLowerCase() in allCommandsList){

                        //add a function to send the server side of information to decide
                        //whether or not should approve the action

                        node.setTag('sink')
                        sink_count = sink_count+1
                        node.setMetaData(exp.text)
                    }
                    if(exp.getMethodAsString()=='definition'){
                        switch (exp.getArguments().getType()) {
                            case ArgumentListExpression:
                                ArgumentListExpression arglistexp = exp.getArguments()
                                MapExpression map = arglistexp.first()
                                List<MapEntryExpression> entrys = map.getMapEntryExpressions()
                                entrys.each {
                                    entry->
                                        def key = entry.getKeyExpression().text
                                        def value = entry.getValueExpression().text
                                        if(key.equalsIgnoreCase('name')){
                                            appName = value
                                        }
                                        else if (key.equalsIgnoreCase('description')){
                                            appDescription = value
                                        }
                                        else if (key.equalsIgnoreCase('category')){
                                            appCategory = value
                                        }
                                }
                                break
                            default:
                                TupleExpression tupleExpression = exp.getArguments()
                                NamedArgumentListExpression namedArgumentListExpression = tupleExpression.first()
                                namedArgumentListExpression.getMapEntryExpressions().each {
                                    entry->
                                        def key = entry.getKeyExpression().text
                                        def value = entry.getValueExpression().text
                                        if(key.equalsIgnoreCase('name')){
                                            appName = value
                                        }
                                        else if (key.equalsIgnoreCase('description')){
                                            appDescription = value
                                        }
                                        else if (key.equalsIgnoreCase('category')){
                                            appCategory = value
                                        }
                                }
                        }
                    }
                }
                nodes.add(node)
            }
        }
    }
    class AnnotationVisitor extends ClassCodeVisitorSupport{
        public ICFG icfg
        public AnnotationVisitor(ICFG cfg) {
            icfg = cfg
        }
        @Override
        protected SourceUnit getSourceUnit() {
            return null;
        }
        @Override
        void visitStatement(Statement statement){
            if(statement.getClass() != BlockStatement && statement.getLineNumber()>0){
                def jsonBuilder = new JsonBuilder()
                ICFGNode node = icfg.getNodeByLineNumber(statement.getLineNumber())
                def node_id = node?.getId()
                def node_tag = node?.getTag()
                def node_data = node?.getMetaData()
                def node_parent = node?.getParent()
                def node_predecessor = node?.getPredecessors()?.size()>0? node.getPredecessors().first():null
                def map = ['id':node_id,'tag':node_tag,'metadata':node_data,'parent':node_parent,'predecessor':node_predecessor]
                jsonBuilder(map)
                def annotation = jsonBuilder.toString()
//                println  'id: ' +node_id+'\t tag:'+node_tag+'\t annotation: '+annotation.replaceAll("\"","\'")
                statement.setStatementLabel(annotation)
            }
        }
    }
    def loadCapRefAll(def file)
    {
        file.splitEachLine(",") { fields ->
            allCommands[fields[0]?.toLowerCase()] = fields[2]?.toLowerCase()
            allProps[fields[0]?.toLowerCase()] = fields[1]?.toLowerCase()
        }

        allCommands.each { k, v ->
            def values = v?.split(" ")
            values?.each { allCommandsList.add(it.toLowerCase()) }
            allCapsList.add(k.toLowerCase())
        }

        allProps.each { k, v ->
            def values = v?.split(" ")
            values?.each { allPropsList.add(it.toLowerCase()) }
        }
        List<String> httpSinks = ['httpDelete','httpGet','httpHead','httpPost','httpPostJson','httpPut','httpPutJson',]
        List<String> notificationSinks = ['sendLocationEvent','sendNotification','sendNotificationEvent','sendNotificationContacts','sendPushMessage','sendSMS','sendSMSMessage','sendPush']
        List<String> changeSettingSinks = ['setLocationMode'] //subscribeToCommand()
        httpSinks.each { allCommandsList.add(it.toLowerCase())}
        notificationSinks.each{ allCommandsList.add(it.toLowerCase())}
        changeSettingSinks.each {allCommandsList.add(it.toLowerCase())}
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
    def toLower
    /**
     * This method creates an empty class node with the qualified name passed as parameter
     *
     * @param qualifiedClassNodeName The qualified name of the ClassNode we want to create
     * @return a new ClassNode instance
     */
}
