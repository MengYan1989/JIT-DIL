package refdiff.core.rm2.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.internal.compiler.util.Util;
import refdiff.core.rm2.model.SDAttribute;
import refdiff.core.rm2.model.SDEntity;
import refdiff.core.rm2.model.SDModel;
import refdiff.core.rm2.model.SDPackage;
import refdiff.core.rm2.model.SDType;
import refdiff.core.rm2.model.SourceRepresentation;

public class SDModelBuilder {
    
    private final RefDiffConfig config;
    private final SourceRepresentationBuilder srbForTypes;
    private final SourceRepresentationBuilder srbForMethods;
    private final SourceRepresentationBuilder srbForAttributes;

    public SDModelBuilder(RefDiffConfig config) {
        this.config = config;
        this.srbForTypes = config.getCodeSimilarityStrategy().createSourceRepresentationBuilderForTypes();
        this.srbForMethods = config.getCodeSimilarityStrategy().createSourceRepresentationBuilderForMethods();
        this.srbForAttributes = config.getCodeSimilarityStrategy().createSourceRepresentationBuilderForAttributes();
    }

    private SDModel model = new SDModel();
    
	private static final String systemFileSeparator = Matcher.quoteReplacement(File.separator);
	
	private Map<SDEntity, List<String>> postProcessReferences;
	private Map<SDEntity, List<String[]>> postProcessReferencesLine;
	private Map<SDType, List<String>> postProcessSupertypes;
	private Map<String, List<SourceRepresentation>> postProcessClientCode;

	private void postProcessReferences(final SDModel.Snapshot model, Map<? extends SDEntity, List<String>> referencesMap) {
	    for (Map.Entry<? extends SDEntity, List<String>> entry : referencesMap.entrySet()) {
	        final SDEntity entity = entry.getKey();
	        List<String> references = entry.getValue();
	        for (String referencedKey : references) {
	            SDEntity referenced = model.findByName(SDEntity.class, referencedKey);
	            if (referenced != null) {
	                entity.addReference(referenced);
	            }
	        }
	    }
	}
	private void postProcessReferencesLine(final SDModel.Snapshot model, Map<? extends SDEntity, List<String[]>> referencesMap) {
	    for (Map.Entry<? extends SDEntity, List<String[]>> entry : referencesMap.entrySet()) {
	        final SDEntity entity = entry.getKey();
	        List<String[]> references = entry.getValue();
	        //List<String> references = aux[0];
	        for (String[] referencedKey : references) {
	            SDEntity referenced = model.findByName(SDEntity.class, referencedKey[0]);
	            if (referenced != null) {
	            	referenced.setCallerline(Long.parseLong(referencedKey[1]));	            	
	            	/*if (entity.simpleName().equals("getKernelNames()")) {
                    	System.out.println("DEBUG");
                    	System.out.println(entity.simpleName() + " - " + referencedKey[0] + "(" + referencedKey[1] + ")");
                    }*/            	
	                entity.addReference(referenced);
	            }
	        }
	    }
	}
	private void postProcessSupertypes(final SDModel.Snapshot model) {
		for (Map.Entry<SDType, List<String>> entry : postProcessSupertypes.entrySet()) {
			final SDType type = entry.getKey();
			List<String> supertypes = entry.getValue();
			for (String supertypeKey : supertypes) {
				SDType supertype = model.findByName(SDType.class, supertypeKey);
				if (supertype != null) {
					supertype.addSubtype(type);
				}
			}
		}
	}
	private void postProcessClientCode(final SDModel.Snapshot model) {
	    for (Map.Entry<String, List<SourceRepresentation>> entry : postProcessClientCode.entrySet()) {
	        final String entityId = entry.getKey();
	        SDAttribute entity = model.findByName(SDAttribute.class, entityId);
	        if (entity != null) {
	            List<SourceRepresentation> sourceSnippets = entry.getValue();
	            if (sourceSnippets.size() > 0) {
	                entity.setClientCode(srbForAttributes.buildSourceRepresentation(entity, sourceSnippets));
	            }
	        }
	    }
	}

	public void analyzeAfter(File rootFolder, List<String> javaFiles) {
	    this.analyze(rootFolder, javaFiles, model.after());
	}

	public void analyzeBefore(File rootFolder, List<String> javaFiles) {
	    this.analyze(rootFolder, javaFiles, model.before());
	    srbForTypes.onComplete();
	    srbForMethods.onComplete();
	    srbForAttributes.onComplete();
	}

	private void analyze(File rootFolder, List<String> javaFiles, final SDModel.Snapshot model) {
	    postProcessReferences = new HashMap<SDEntity, List<String>>();
	    postProcessReferencesLine = new HashMap<SDEntity, List<String[]>>();
		postProcessSupertypes = new HashMap<SDType, List<String>>();
		postProcessClientCode = new HashMap<String, List<SourceRepresentation>>();
		final String projectRoot = rootFolder.getPath();
		final String[] emptyArray = new String[0];
		
		String[] filesArray = new String[javaFiles.size()];
		for (int i = 0; i < filesArray.length; i++) {
			filesArray[i] = rootFolder + File.separator + javaFiles.get(i).replaceAll("/", systemFileSeparator);
		}
		final String[] sourceFolders = this.inferSourceFolders(filesArray);
		final ASTParser parser = buildAstParser(sourceFolders);

		FileASTRequestor fileASTRequestor = new FileASTRequestor() { 
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit ast) {
				String relativePath = sourceFilePath.substring(projectRoot.length() + 1).replaceAll(systemFileSeparator, "/");
//				IProblem[] problems = ast.getProblems();
//				if (problems.length > 0) {
//					System.out.println("problems");
//				}
				//
				try {
					char[] charArray = Util.getFileCharContent(new File(sourceFilePath), null);
					processCompilationUnit(relativePath, charArray, ast, model);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
//				catch (DuplicateEntityException e) {
		            // debug e;
//		        }
			}
		};
		
		System.out.println("Starting create AST method in " + new Date());
		parser.createASTs((String[]) filesArray, null, emptyArray, fileASTRequestor, null);
		System.out.println("Finished create AST method in " + new Date());
		
		/*
		postProcessReferences(model, postProcessReferences);
		postProcessReferences = null;
		/*/
		postProcessReferencesLine(model, postProcessReferencesLine);
		postProcessReferencesLine = null;
		
		postProcessSupertypes(model);
		postProcessSupertypes = null;
		postProcessClientCode(model);
		postProcessClientCode = null;
	}

	private static ASTParser buildAstParser(String[] sourceFolders) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setEnvironment(new String[0], sourceFolders, null, true);
		//parser.setEnvironment(new String[0], new String[]{"tmp\\refactoring-toy-example\\src"}, null, false);
		return parser;
	}

	protected void processCompilationUnit(String sourceFilePath, char[] fileContent, CompilationUnit compilationUnit, SDModel.Snapshot model) {
		PackageDeclaration packageDeclaration = compilationUnit.getPackage();
		String packageName = "";
		if (packageDeclaration != null) {
			packageName = packageDeclaration.getName().getFullyQualifiedName();
		}
		String packagePath = packageName.replace('.', '/');
		String sourceFolder = "/";
		if (sourceFilePath.contains(packagePath)) {
		  sourceFolder = sourceFilePath.substring(0, sourceFilePath.indexOf(packagePath));
		}
		SDPackage sdPackage = model.getOrCreatePackage(packageName, sourceFolder);
		BindingsRecoveryAstVisitor visitor = new BindingsRecoveryAstVisitor(compilationUnit, model, sourceFilePath, fileContent, sdPackage, postProcessReferences, postProcessReferencesLine, postProcessSupertypes, postProcessClientCode, srbForTypes, srbForMethods, srbForAttributes);
		//System.out.println(compilationUnit.getLineNumber(1767)-1);
		compilationUnit.accept(visitor);
		//System.out.println(compilationUnit.getLineNumber(1767));
	}

	private String[] inferSourceFolders(String[] filesArray) {
		Set<String> sourceFolders = new TreeSet<String>();
		nextFile: for (String file : filesArray) {
			for (String sourceFolder : sourceFolders) {
				if (file.startsWith(sourceFolder)) {
					continue nextFile;
				}
			}
			String otherSourceFolder = extractSourceFolderFromPath(file);
			if (otherSourceFolder != null) {
				sourceFolders.add(otherSourceFolder);
//				System.out.print("source folder: ");
//				System.out.println(otherSourceFolder);
			}
		}
		return sourceFolders.toArray(new String[sourceFolders.size()]);
	}

	private String extractSourceFolderFromPath(String sourceFilePath) {
		try (BufferedReader scanner = new BufferedReader(new FileReader(sourceFilePath))) {
			String lineFromFile;
			while ((lineFromFile = scanner.readLine()) != null) {
				if (lineFromFile.startsWith("package ")) { 
					// a match!
					//System.out.print("package declaration: ");
					String packageName = lineFromFile.substring(8, lineFromFile.indexOf(';'));
					//System.out.println(packageName);
					
					String packagePath = packageName.replace('.', File.separator.charAt(0));
					int indexOfPackagePath = sourceFilePath.lastIndexOf(packagePath + File.separator);
					if (indexOfPackagePath >= 0) {
						return sourceFilePath.substring(0, indexOfPackagePath - 1);
					}
					return null;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}
	
	/////////////////
	
    public SDModel buildModel() {
        model.initRelationships();
        RefactoringDetector rbuilder = new RefactoringDetector(config);
        rbuilder.analyze(model);
        return model;
    }
	
}
