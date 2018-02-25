/*
 * 
 */



import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.SourceUnit

class InsnVisitor extends ClassCodeVisitorSupport 
{
	Set<String> calledMethods
	Set<String> calledProps
	Set<String> requestedCaps
	
	public InsnVisitor()
	{
		calledMethods = new HashSet<String>()
		calledProps = new HashSet<String>()
		requestedCaps = new HashSet<String>()
	}
	
	@Override
	void visitMethodCallExpression(MethodCallExpression mce)
	{
		//println mce.getMethodAsString()
		calledMethods.add(mce.getMethodAsString()?.toLowerCase())
		
		if(mce.getMethodAsString().equals("input"))
		{
			def args = mce.getArguments()
			List cexp = new ArrayList()
			
			args.each { arg ->
				if(arg instanceof ConstantExpression)
				{
					cexp.add((ConstantExpression) arg)					
				}
			}
			
			requestedCaps.add(cexp[1].getText().toLowerCase())
			
			
			
		}
		
		super.visitMethodCallExpression(mce)
	}
	
	@Override
	void visitPropertyExpression(PropertyExpression pe)
	{
		calledProps.add(pe.getPropertyAsString().toLowerCase())
		
		super.visitPropertyExpression(pe)
	}
	
	@Override
	protected SourceUnit getSourceUnit() {
		return null;
	}
}
