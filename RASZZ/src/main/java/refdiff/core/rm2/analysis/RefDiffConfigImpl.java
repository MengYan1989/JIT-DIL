package refdiff.core.rm2.analysis;

import java.util.HashMap;
import java.util.Map;

import refdiff.core.rm2.analysis.codesimilarity.CodeSimilarityStrategy;
import refdiff.core.rm2.model.RelationshipType;

public class RefDiffConfigImpl implements RefDiffConfig {

    private String id = "refdiff";

    private double defaultThreshold = 0.5;
    
    private Map<RelationshipType, Double> thresholds = new HashMap<>();
    
    private CodeSimilarityStrategy codeSimilarityStrategy = CodeSimilarityStrategy.TFIDF;

    public RefDiffConfigImpl() {
        setThreshold(RelationshipType.MOVE_TYPE, 0.9);
        setThreshold(RelationshipType.RENAME_TYPE, 0.4);
        setThreshold(RelationshipType.EXTRACT_SUPERTYPE, 0.8);
        setThreshold(RelationshipType.MOVE_METHOD, 0.4);
        setThreshold(RelationshipType.RENAME_METHOD, 0.3);
        setThreshold(RelationshipType.PULL_UP_METHOD, 0.4);
        setThreshold(RelationshipType.PUSH_DOWN_METHOD, 0.6);
        setThreshold(RelationshipType.EXTRACT_METHOD, 0.1);
        setThreshold(RelationshipType.INLINE_METHOD, 0.3);
        setThreshold(RelationshipType.MOVE_FIELD, 0.5);
        setThreshold(RelationshipType.PULL_UP_FIELD, 0.5);
        setThreshold(RelationshipType.PUSH_DOWN_FIELD, 0.3);
    }

    @Override
    public double getThreshold(RelationshipType relationshipType) {
        if (thresholds.containsKey(relationshipType)) {
            return thresholds.get(relationshipType);
        }
        return defaultThreshold;
    }

    public void setThreshold(RelationshipType relationshipType, double value) {
        thresholds.put(relationshipType, value);
    }

    public double getDefaultThreshold() {
        return defaultThreshold;
    }

    public void setDefaultThreshold(double defaultThreshold) {
        this.defaultThreshold = defaultThreshold;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public CodeSimilarityStrategy getCodeSimilarityStrategy() {
        return codeSimilarityStrategy;
    }

    public RefDiffConfig setCodeSimilarityStrategy(CodeSimilarityStrategy codeSimilarityStrategy) {
        this.codeSimilarityStrategy = codeSimilarityStrategy;
        return this;
    }

    @Override
    public RefDiffConfigImpl clone() {
        RefDiffConfigImpl c = new RefDiffConfigImpl();
        c.id = id;
        c.defaultThreshold = defaultThreshold;
        c.codeSimilarityStrategy = codeSimilarityStrategy;
        c.thresholds = new HashMap<>(thresholds);
        return c;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RefactoringDetectorConfig(");
        sb.append(id);
        sb.append(", ");
        sb.append(codeSimilarityStrategy.toString());
        sb.append(")");
        for (RelationshipType type : RelationshipType.values()) {
            if (thresholds.containsKey(type)) {
                sb.append("\n\tconfig.setThreshold(RelationshipType.");
                sb.append(type.toString());
                sb.append(", ");
                sb.append(thresholds.get(type));
                sb.append(");");
            }
        }
        return sb.toString();
    }
}