package refdiff.core.rm2.model.refactoring;

import refdiff.core.rm2.model.SDMethod;

import refdiff.core.api.RefactoringType;

public class SDInlineMethod extends SDRefactoring {

    private final SDMethod inlinedMethod;
    private final SDMethod dest;
    
    public SDInlineMethod(SDMethod inlinedMethod, SDMethod dest) {
        super(RefactoringType.INLINE_OPERATION, inlinedMethod, inlinedMethod, dest);
        this.inlinedMethod = inlinedMethod;
        this.dest = dest;
    }

    //Inline Method private getModuleFileName() : String inlined to public resolve() : PsiElement in class org.intellij.erlang.psi.impl.ErlangFunctionReferenceImpl
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(this.inlinedMethod.getVerboseSimpleName());
        sb.append(" inlined to ");
        sb.append(dest.getVerboseSimpleName());
        sb.append(" in class ");
        sb.append(dest.container().fullName());
        return sb.toString();
    }
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(this.getName());
//        sb.append(' ');
//        sb.append(this.inlinedMethod.getVerboseSimpleName());
//        sb.append(" inlined to ");
//        Multiset<SDMethod> sameClassDestinations = inlinedMethod.inlinedTo().suchThat(new Filter<SDMethod>(){
//            public boolean accept(SDMethod m) {
//                return m.container().equals(inlinedMethod.container());
//            }
//        });
//        SDMethod dest;
//        if (sameClassDestinations.size() > 0) {
//            dest = sameClassDestinations.getFirst();
//        } else {
//            dest = inlinedMethod.inlinedTo().getFirst();
//        }
//        sb.append(dest.getVerboseSimpleName());
//        sb.append(" in class ");
//        sb.append(dest.container().fullName());
//        return sb.toString();
//    }
}
