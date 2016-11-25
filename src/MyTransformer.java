import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.util.*;
import java.util.*;


class MyTransformer extends BodyTransformer {

    // the name of the tracker class. we need to insert calls to
    // methods from this tracker class
    private static String tracker_name = "RuntimeTracker";

    // SootClass object representing the tracker class
    private static SootClass tracker_class;

    // the method in the tracker class that should be invoked at the
    // entry of each instrumented method.
    private static SootMethod method_entry;

    // some Soot-related code (since we are inheriting from Soot's
    // BodyTransformer)
    private static MyTransformer instance = new MyTransformer();
    private MyTransformer() {}
    public static MyTransformer v() { return instance; }
    public String getDeclaredOptions() { return super.getDeclaredOptions(); }

    // ------------------------------------------------
    // process a method body and insert instrumentation

    protected void internalTransform(Body body, 
				     String phaseName, 
				     Map options) {

	// the SootMethod we are currently instrumenting
	SootMethod method = body.getMethod();

	// initialize the static fields related to the tracker class
	if (tracker_class == null) {
	    tracker_class = 
		Scene.v().getSootClass(tracker_name);

	    method_entry = 
		tracker_class.getMethod("void methodEntry(int)");
	}

	// no instrumentation will be inserted in the body of the
	// tracker classs
	if (method.getDeclaringClass().
	    getName().equals(tracker_name)) return;

	// the id of this method, as read from "rmethods.cut"
	String method_id = (String) 
	    Instrumenter.id_info.get(method.toString());

	// for methods that are not in file rmethods.cut (i.e. methods
	// that are non-reachable or non-CUT), there is no id and we
	// shouldn't instrument
	if (method_id == null) return;

	// the number of parameters (including 'this'). we need this
	// to figure out the right place for method-entry
	// instrumentation - the first num_param JIMPLE statements
	// should be skipped [you don't really need to understand why
	// this is necessary].
	int num_param = method.getParameterCount();
	// for non-static methods, need to take into account 'this'
	if (!method.isStatic()) num_param++;

	Chain units = body.getUnits();
	Iterator stmtIt = units.snapshotIterator();
	Stmt s = null;
      
	// first, skip the initial assignments to parameters.
	for (int i = 0; i < num_param;  i++) {
	    s = (Stmt) stmtIt.next();
	    // sanity check; ignore it
	    Assert(s instanceof IdentityStmt, 
		   ("Expected IdentityStmt: " + s));
	}
	
	// another sanity check: there should be at least one
	// statement after this
	Assert(stmtIt.hasNext(),"Empty Body");

	// the first "real" statement
	s = (Stmt) stmtIt.next();
	
	// create a JIMPLE staticinvoke expression that calls
	// "methodEntry" in the tracker class. The actual parameter of
	// the call is the method id of the method whose body we are
	// currenty processing.
	int m_id = Integer.parseInt(method_id);
	StaticInvokeExpr sc = 
	    Jimple.v().newStaticInvokeExpr(method_entry,IntConstant.v(m_id));

	// insert the staticinvoke before the first real statement
	units.insertBefore(Jimple.v().newInvokeStmt(sc),s);

	// process all statements, starting with the first one.
	// insert instrumentation before each call site. 
	int call_site_id = 1;
	boolean first_iter = true;
	do {
	    // since we already have the first real statement from
	    // stmtIt, we shouldn't call stmtIt.next() during the
	    // first iteration
	    if (first_iter) first_iter = false;
	    else s = (Stmt) stmtIt.next();		

	    // instrument all calls
	    if (s.containsInvokeExpr()) { 

		InvokeExpr c = (InvokeExpr) s.getInvokeExpr();
		
		// ???

	    }	
	} while (stmtIt.hasNext());
    }

    // -------------------------------
    void Assert(boolean x, String s) {
	// there are better ways to do this
	if (!x) throw new RuntimeException(s);
    }
}
