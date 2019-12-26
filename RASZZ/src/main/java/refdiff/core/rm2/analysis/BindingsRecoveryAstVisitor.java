package refdiff.core.rm2.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
//import org.neo4j.cypher.internal.compiler.v2_3.functions.Has;

import refdiff.core.rm2.model.SDAttribute;
import refdiff.core.rm2.model.SDContainerEntity;
import refdiff.core.rm2.model.SDEntity;
import refdiff.core.rm2.model.SDMethod;
import refdiff.core.rm2.model.SDModel;
import refdiff.core.rm2.model.SDPackage;
import refdiff.core.rm2.model.SDType;
import refdiff.core.rm2.model.SourceRepresentation;
import refdiff.core.rm2.model.Visibility;
import refdiff.core.util.AstUtils;

public class BindingsRecoveryAstVisitor extends ASTVisitor {

    private final SDModel.Snapshot model;
    private final String sourceFilePath;
    private final char[] fileContent;
    private final LinkedList<SDContainerEntity> containerStack;
    private final Map<SDEntity, List<String>> postProcessReferences;
    private final Map<SDEntity, List<String[]>> postProcessReferencesLine;
    private final Map<SDType, List<String>> postProcessSupertypes;
    private final Map<String, List<SourceRepresentation>> postProcessClientCode;
    private final SourceRepresentationBuilder srbForTypes;
    private final SourceRepresentationBuilder srbForMethods;
    private final SourceRepresentationBuilder srbForAttributes;
    private final CompilationUnit compilationUnit;

    public BindingsRecoveryAstVisitor(CompilationUnit compilationUnit, SDModel.Snapshot model, String sourceFilePath, char[] fileContent, SDPackage sdPackage, Map<SDEntity, List<String>> postProcessReferences, Map<SDEntity, List<String[]>> postProcessReferencesLine,
        Map<SDType, List<String>> postProcessSupertypes, Map<String, List<SourceRepresentation>> postProcessClientCode, SourceRepresentationBuilder srbForTypes, SourceRepresentationBuilder srbForMethods, SourceRepresentationBuilder srbForAttributes) {
        this.model = model;
        this.sourceFilePath = sourceFilePath;
        this.fileContent = fileContent;
        this.containerStack = new LinkedList<SDContainerEntity>();
        this.containerStack.push(sdPackage);
        this.postProcessReferences = postProcessReferences;
        this.postProcessSupertypes = postProcessSupertypes;
        this.postProcessClientCode = postProcessClientCode;
        this.postProcessReferencesLine = postProcessReferencesLine;
        this.srbForTypes = srbForTypes;
        this.srbForMethods = srbForMethods;
        this.srbForAttributes = srbForAttributes;
        this.compilationUnit = compilationUnit;
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        if (node.getParent() instanceof ClassInstanceCreation) {
            ClassInstanceCreation parent = (ClassInstanceCreation) node.getParent();
            ITypeBinding typeBinding = parent.getType().resolveBinding();
            if (typeBinding != null && typeBinding.isFromSource()) {
                SDType type = model.createAnonymousType(containerStack.peek(), sourceFilePath, "");
                containerStack.push(type);
                extractSupertypesForPostProcessing(type, typeBinding);
                return true;
            }
        }
        return false;
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
        if (node.getParent() instanceof ClassInstanceCreation) {
            ClassInstanceCreation parent = (ClassInstanceCreation) node.getParent();
            ITypeBinding typeBinding = parent.getType().resolveBinding();
            if (typeBinding != null && typeBinding.isFromSource()) {
                containerStack.pop();
            }
        }
    }

    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        containerStack.push(visitTypeDeclaration(node, node.superInterfaceTypes()));
        return true;
    }

    public void endVisit(EnumDeclaration node) {
        containerStack.pop();
    }

    public boolean visit(TypeDeclaration typeDeclaration) {
        List<Type> supertypes = new ArrayList<Type>();
        Type superclass = typeDeclaration.getSuperclassType();
        if (superclass != null) {
            supertypes.add(superclass);
        }
        supertypes.addAll(typeDeclaration.superInterfaceTypes());
        SDType sdType = visitTypeDeclaration(typeDeclaration, supertypes);
        containerStack.push(sdType);
        sdType.setIsInterface(typeDeclaration.isInterface());
        return true;
    }

    public void endVisit(TypeDeclaration node) {
        containerStack.pop();
    }

    private SDType visitTypeDeclaration(AbstractTypeDeclaration node, List<Type> supertypes) {
        SDType type;
        String typeName = node.getName().getIdentifier();
        if (node.isLocalTypeDeclaration()) {
            type = model.createAnonymousType(containerStack.peek(), sourceFilePath, typeName);
        } else {
            type = model.createType(typeName, containerStack.peek(), sourceFilePath);
        }
        //type.setSourceCode(srbForTypes.buildSourceRepresentation(fileContent, node.getStartPosition(), node.getLength()));
        type.setSourceCode(srbForTypes.buildSourceRepresentation(type, fileContent, node));
        //System.out.println(srbForTypes.extractsourcecode(fileContent, node.getStartPosition(), node.getStartPosition()));

        Set<String> annotations = extractAnnotationTypes(node.modifiers());
        type.setDeprecatedAnnotation(annotations.contains("Deprecated"));

        type.setStartLine(compilationUnit.getLineNumber(node.getStartPosition()));
        type.setEndLine(compilationUnit.getLineNumber(node.getStartPosition() + node.getStartPosition())); 
        type.setFirstpositionstatement(compilationUnit.getLineNumber(AstUtils.startPositionFirstStatement(node)));
        
        for (Type superType : supertypes) {
            ITypeBinding superTypeBinding = superType.resolveBinding();
            extractSupertypesForPostProcessing(type, superTypeBinding);
        }

        final List<String> references = new ArrayList<String>();
        final List<String[]> referencesline = new ArrayList<String[]>();
        node.accept(new DependenciesAstVisitor(true) {
            @Override
            protected void onTypeAccess(ASTNode node, ITypeBinding binding) {
                String typeKey = AstUtils.getKeyFromTypeBinding(binding);
                references.add(typeKey);
                String[] ref = {typeKey, Integer.toString(compilationUnit.getLineNumber(node.getStartPosition()))};
                referencesline.add(ref);                
                //System.out.println(binding.getKey() + " - " + node.toString() + "(" + compilationUnit.getLineNumber(node.getStartPosition()) + ")");
            }
        });
        postProcessReferences.put(type, references);
        postProcessReferencesLine.put(type, referencesline);

        return type;
    }

    private void extractSupertypesForPostProcessing(SDType type, ITypeBinding superTypeBinding) {
        List<String> supertypes = postProcessSupertypes.get(type);
        if (supertypes == null) {
            supertypes = new ArrayList<String>();
            postProcessSupertypes.put(type, supertypes);
        }
        while (superTypeBinding != null && superTypeBinding.isFromSource()) {
            String superTypeName = superTypeBinding.getErasure().getQualifiedName();
            supertypes.add(superTypeName);
            superTypeBinding = superTypeBinding.getSuperclass();
        }
    }

    public boolean visit(MethodDeclaration methodDeclaration) {
        // ASTNode parentNode = methodDeclaration.getParent();
        // if (!(parentNode instanceof TypeDeclaration)) {
        // // ignore methods from anonymous classes
        // return false;
        // }
        String methodSignature = AstUtils.getSignatureFromMethodDeclaration(methodDeclaration);
        // if
        // (methodDeclaration.getName().getIdentifier().equals("execMultiLineCommands"))
        // {
        // System.out.println("x");
        //
        // }
        
        final SDMethod method = model.createMethod(methodSignature, containerStack.peek(), methodDeclaration.isConstructor());
                
        method.setStartLine(compilationUnit.getLineNumber(methodDeclaration.getStartPosition()));
        method.setEndLine(compilationUnit.getLineNumber(methodDeclaration.getStartPosition() + methodDeclaration.getLength()));
        
        List<?> modifiers = methodDeclaration.modifiers();
        Set<String> annotations = extractAnnotationTypes(modifiers);
        method.setTestAnnotation(annotations.contains("Test"));
        method.setDeprecatedAnnotation(annotations.contains("Deprecated") || AstUtils.containsDeprecatedTag(methodDeclaration.getJavadoc()));

        int methodModifiers = methodDeclaration.getModifiers();
        Visibility visibility = getVisibility(methodModifiers);

        method.setVisibility(visibility);
        extractParametersAndReturnType(methodDeclaration, method);

        method.setFirstpositionstatement(compilationUnit.getLineNumber(AstUtils.startPositionFirstStatement(methodDeclaration)));
        //method.setFirststatement(AstUtils.firstStatement(methodDeclaration));
        method.setContent(methodDeclaration.toString());
        //System.out.println(method.getContent());
          
        method.setNumberOfStatements(AstUtils.countNumberOfStatements(methodDeclaration));
        Block body = methodDeclaration.getBody();
        if (body == null) {
            method.setSourceCode(srbForMethods.buildEmptySourceRepresentation());
            method.setAbstract(true);
        } else {
            //method.setSourceCode(srbForMethods.buildSourceRepresentation(this.fileContent, body.getStartPosition() + 1, body.getLength() - 2));
            method.setSourceCode(srbForMethods.buildSourceRepresentation(method, this.fileContent, body));
            final List<String> references = new ArrayList<String>();
            final List<String[]> referencesline = new ArrayList<String[]>();
            body.accept(new DependenciesAstVisitor(true) {
                @Override
                protected void onMethodAccess(ASTNode node, IMethodBinding binding) {
                    String methodKey = AstUtils.getKeyFromMethodBinding(binding);
                    references.add(methodKey);
                    String[] ref = {methodKey, Integer.toString(compilationUnit.getLineNumber(node.getStartPosition()))};
                    referencesline.add(ref); 
                    /*if (method.identifier().equals("getKernelNames")) {
                    	System.out.println("DEBUG");
                    	System.out.println(method.identifier() + " - " + node.toString() + "(" + compilationUnit.getLineNumber(node.getStartPosition()) + ")");
                    }*/
                }

                @Override
                protected void onFieldAccess(ASTNode node, IVariableBinding binding) {
                    String attributeKey = AstUtils.getKeyFromFieldBinding(binding);
                    references.add(attributeKey);
                    String[] ref = {attributeKey, Integer.toString(compilationUnit.getLineNumber(node.getStartPosition()))};
                    referencesline.add(ref); 
                    /*if (method.identifier().equals("getKernelNames")) {
                    	System.out.println("DEBUG");
                    	System.out.println(method.identifier() + " - " + node.toString() + "(" + compilationUnit.getLineNumber(node.getStartPosition()) + ") - " + attributeKey);
                    }*/

                    Statement stm = AstUtils.getEnclosingStatement(node);
                    // if (stm == null) {
                    // System.out.println("null");
                    // }
                    SourceRepresentation code = srbForAttributes.buildPartialSourceRepresentation(fileContent, stm);
                    addClientCode(attributeKey, code);
                }

            });
            postProcessReferences.put(method, references);
            postProcessReferencesLine.put(method, referencesline);
        }

        return true;
    }

    private void addClientCode(String attributeKey, SourceRepresentation code) {
        List<SourceRepresentation> codeFragments = postProcessClientCode.get(attributeKey);
        if (codeFragments == null) {
            codeFragments = new ArrayList<SourceRepresentation>();
        }
        codeFragments.add(code);
        postProcessClientCode.put(attributeKey, codeFragments);
    }

    private Visibility getVisibility(int methodModifiers) {
        Visibility visibility;
        if ((methodModifiers & Modifier.PUBLIC) != 0)
            visibility = Visibility.PUBLIC;
        else if ((methodModifiers & Modifier.PROTECTED) != 0)
            visibility = Visibility.PROTECTED;
        else if ((methodModifiers & Modifier.PRIVATE) != 0)
            visibility = Visibility.PRIVATE;
        else
            visibility = Visibility.PACKAGE;
        return visibility;
    }

    @Override
    public boolean visit(FieldDeclaration fieldDeclaration) {
        Type fieldType = fieldDeclaration.getType();
        int fieldModifiers = fieldDeclaration.getModifiers();
        Visibility visibility = getVisibility(fieldModifiers);
        // boolean isFinal = (fieldModifiers & Modifier.FINAL) != 0;
        boolean isStatic = (fieldModifiers & Modifier.STATIC) != 0;
        List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            String fieldName = fragment.getName().getIdentifier();
            final SDAttribute attribute = model.createAttribute(fieldName, containerStack.peek());
            attribute.setStatic(isStatic);
            attribute.setVisibility(visibility);
            attribute.setType(AstUtils.normalizeTypeName(fieldType, fragment.getExtraDimensions(), false));
           
            attribute.setStartLine(compilationUnit.getLineNumber(fieldDeclaration.getStartPosition()));
            attribute.setEndLine(compilationUnit.getLineNumber(fieldDeclaration.getStartPosition() + fieldDeclaration.getLength()));
            attribute.setContent(fieldDeclaration.toString());
            
            Expression expression = fragment.getInitializer();
            if (expression != null) {
                //attribute.setAssignment(srbForAttributes.buildSourceRepresentation(fileContent, expression.getStartPosition(), expression.getLength()));
                addClientCode(attribute.key().toString(), srbForAttributes.buildPartialSourceRepresentation(fileContent, expression));
            }
            attribute.setClientCode(srbForAttributes.buildEmptySourceRepresentation());
        }
        return true;
    }

    private static Set<String> extractAnnotationTypes(List<?> modifiers) {
        Set<String> annotations = new HashSet<String>();
        for (Object modifier : modifiers) {
            if (modifier instanceof Annotation) {
                Annotation a = (Annotation) modifier;
                annotations.add(a.getTypeName().toString());
            }
        }
        return annotations;
    }

    public static void extractParametersAndReturnType(MethodDeclaration methodDeclaration, SDMethod method) {
        Type returnType = methodDeclaration.getReturnType2();
        if (returnType != null) {
            method.setReturnType(AstUtils.normalizeTypeName(returnType, methodDeclaration.getExtraDimensions(), false));
        } else {
            method.setReturnType(null);
        }
        Iterator<SingleVariableDeclaration> parameters = methodDeclaration.parameters().iterator();
        while (parameters.hasNext()) {
            SingleVariableDeclaration parameter = parameters.next();
            Type parameterType = parameter.getType();
            String typeName = AstUtils.normalizeTypeName(parameterType, parameter.getExtraDimensions(), parameter.isVarargs());
            method.addParameter(parameter.getName().getIdentifier(), typeName);
        }
    }

    // @Override
    // public void endVisit(MethodDeclaration methodDeclaration) {
    // if
    // (methodDeclaration.getName().toString().endsWith("testFixedMembershipTokenIPv4"))
    // {
    // System.out.print(' ');
    // }
    // }

    // private String getTypeName(Type type) {
    // ITypeBinding binding = type.resolveBinding();
    // ITypeBinding y = binding;
    // while (y != null && y.isFromSource()) {
    // System.out.println(y.getQualifiedName());
    // y = y.getSuperclass();
    // }
    //
    // if (binding != null) {
    // return binding.getQualifiedName();
    // }
    // return type.toString();
    // }

}
