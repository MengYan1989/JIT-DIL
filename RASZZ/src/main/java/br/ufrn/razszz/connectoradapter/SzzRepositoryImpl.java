package br.ufrn.razszz.connectoradapter;

import static br.ufrn.raszz.util.FileOperationsUtil.isTestFile;
import static br.ufrn.raszz.util.FileOperationsUtil.runRegex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.LinkedList;

import br.ufrn.raszz.model.RepositoryType;
import br.ufrn.raszz.model.szz.SzzFileRevision;
import br.ufrn.raszz.util.FileOperationsUtil;
import org.apache.commons.lang3.StringUtils;

public abstract class SzzRepositoryImpl implements SzzRepository {
	
	protected RepositoryType connectorType;
	protected String url;
	protected String user;
	protected String password;
	protected String repositoryFolder;	
	
	@Override
	public final RepositoryType getConnectorType(){
		return connectorType;
	}
	
	@Override
	public final String getUrl(){
		return url;
	}
	
	@Override
	public final String getUser(){
		return user;
	}
	
	@Override
	public final String getPassword(){
		return password;
	}
	
	@Override
	public final String getRepositoryFolder(){
		return repositoryFolder;
	}
	
	@Override
	public final LinkedList<SzzFileRevision> extractSZZFilesFromPath(String repoUrl, String path, String revision,
																	 boolean isReTrace) throws Exception {
		String fname = FileOperationsUtil.getFileName(path);
		if(fname == null || !fname.contains(".java") || fname.trim().endsWith(".javajet")) return null;
		// 新加条件
		// 路径中含有test即排除
		String[] subPaths = StringUtils.split(path, '/');
		for (String sp: subPaths) {
			String tempSP = sp.toLowerCase();
			if (tempSP.equals("test") || tempSP.equals("tests"))
				return null;
		}

		ByteArrayOutputStream baous = catOperation(repoUrl, path, revision);
		
		//we are also not interested on testFiles
		if(runRegex(path,"Test.java$") || isTestFile(baous)){
			return null;
		}
		
		final LinkedList<SzzFileRevision> szzFileRevisions = getSzzFileRevisions(path, revision);
		
		if(!isReTrace && szzFileRevisions.size() == 1) return null;		
		
		return szzFileRevisions;
	}
	
	protected abstract LinkedList<SzzFileRevision> getSzzFileRevisions(String path, String commitId) throws Exception;

}
