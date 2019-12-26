package refdiff.core.rm2.model.refactoring;

import refdiff.core.rm2.model.SDType;

import refdiff.core.api.RefactoringType;

public class SDMoveClass extends SDRefactoring {

    private final SDType typeBefore;
    private final SDType typeAfter;
    
    public SDMoveClass(SDType typeBefore, SDType typeAfter) {
        this(RefactoringType.MOVE_CLASS, typeBefore, typeAfter);
    }

    protected SDMoveClass(RefactoringType type, SDType typeBefore, SDType typeAfter) {
        super(type, typeBefore, typeBefore, typeAfter);
        this.typeBefore = typeBefore;
        this.typeAfter = typeAfter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName());
        sb.append(' ');
        sb.append(typeBefore.fullName());
        sb.append(" moved to ");
        sb.append(typeAfter.fullName());
        return sb.toString();
    }
}
