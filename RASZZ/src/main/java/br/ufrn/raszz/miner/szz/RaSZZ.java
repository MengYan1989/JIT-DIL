package br.ufrn.raszz.miner.szz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.ufrn.raszz.persistence.HibernateUtil;
import br.ufrn.raszz.util.CsvOperationsUtil;
import cs.zju.config.Config;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.tmatesoft.svn.core.io.SVNRepository;

import br.ufrn.raszz.miner.Miner;
import br.ufrn.raszz.model.RepositoryType;
import br.ufrn.raszz.model.SZZImplementationType;
import br.ufrn.raszz.persistence.DAOType;
import br.ufrn.raszz.persistence.FactoryDAO;
import br.ufrn.raszz.persistence.SzzDAO;
import br.ufrn.razszz.connectoradapter.GitRepositoryAdapter;
import br.ufrn.razszz.connectoradapter.SvnRepositoryAdapter;
import br.ufrn.razszz.connectoradapter.SzzRepository;
import core.connector.service.GitService;
import core.connector.service.SvnService;
import core.connector.service.impl.GitServiceImpl;
import core.connector.service.impl.SvnServiceImpl;

public class RaSZZ extends Miner {

	private static final Logger log = Logger.getLogger(RaSZZ.class);
	private SzzDAO szzDAO;
	private SzzRepository repository;

	public static void main(String[] args) throws Exception {
		RaSZZ szz = new RaSZZ();
		String[] projects = {"?"};
		RepositoryType repoType = RepositoryType.GIT;
		SZZImplementationType szzType = SZZImplementationType.RASZZ;
		szz.init(projects, repoType, szzType, false, null);
	}	
	
	public void init(String[] projects, RepositoryType repoType, SZZImplementationType szzType, boolean isTest, String[] debugInfos) throws Exception {
		String user = Config.getStringProperty("user");
		String password = Config.getStringProperty("password");
		String tmpfolder =  Config.getStringProperty("tmpfolder");
		boolean entireDb = Config.getBooleanProperty("entire_db");

		String url = "";
		switch (repoType) {
			// 我们只关注Git
			case GIT:
				tmpfolder += "gitfiles//" + projects[0];
				url = "?";
				break;
			case SVN:
				tmpfolder += "svnfiles\\";
				url = this.getProperty("svn_url","./backhoe.properties");
				break;
		}	

		String linkedRev = null, debugPath = null, debugContent = null;
		if (isTest) {
			linkedRev = debugInfos[0];
			debugPath = debugInfos[1]; 
			debugContent = debugInfos[2];
		} 
		
		Map<String, Object> p = new HashMap<String, Object>();
		try {
			p.put("user", user);
			p.put("password", password);
			p.put("tmpfolder", tmpfolder);
			p.put("repoType", repoType);
			p.put("szzType", szzType);
			p.put("repoUrl", url);
			p.put("projects", projects);
			p.put("entireDb", entireDb);
			p.put("isTest", isTest);			
			p.put("debugRev", linkedRev);
			p.put("debugPath", debugPath);
			p.put("debugContent", debugContent);
			this.setParameters(p);
			this.executeMining();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	

	@Override
	public void performSetup() throws Exception {
		System.out.println("perform setup ... ");
		try {
			String user = (String) this.getParameters().get("user");
			String password = (String) this.getParameters().get("password");
			String tmpfolder = (String) this.getParameters().get("tmpfolder");
			RepositoryType repoType = (RepositoryType) this.getParameters().get("repoType");
			String repoUrl = (String) this.getParameters().get("repoUrl");
			
			switch (repoType) {
			case GIT:
				System.out.println("Create project repository");
				GitService gitService = new GitServiceImpl();
				Repository gitRepository = gitService.cloneIfNotExists(tmpfolder, repoUrl);
				repository = new GitRepositoryAdapter(gitRepository, repoUrl, tmpfolder);
				break;
			case SVN:
				SvnService svnService = new SvnServiceImpl();
				SVNRepository svnRepository = svnService.openRepository(repoUrl, user, password);
				repository = new SvnRepositoryAdapter(svnRepository, user, password, repoUrl, tmpfolder);
				break;
			}
			
			szzDAO = (FactoryDAO.getFactoryDAO(DAOType.HIBERNATE)).getSzzDAO();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void performMining() throws Exception {
		System.out.println("perform mining...");
		// 是否构建Annotation Graph
		final boolean buildAnnotationGraph = Config.getBooleanProperty("build_graph");
		System.out.println("Start Building Annotation Graph");
		if (buildAnnotationGraph) 
			buildAnnotationGraph();
	}
	
	private void buildAnnotationGraph() throws Exception {
		try {
			String[] projects = (String[]) this.getParameters().get("projects");
			boolean entireDb = (Boolean) this.getParameters().get("entireDb");
			String repoUrl = (String) this.getParameters().get("repoUrl");
			RepositoryType repoType = (RepositoryType) this.getParameters().get("repoType");	
			
			for (int j = 0; j < projects.length; j++) {
				// 该项目的全部fix
				List<String> linkedRevs = CsvOperationsUtil.getFixCommits(projects[j]);
				String debugPath = null;
				String debugContent = null;
				boolean isTest = (Boolean) this.getParameters().get("isTest");				
				if (isTest) {
					String linkedTestRev = (String) this.getParameters().get("debugRev");
					linkedRevs = linkedRevs.stream()
						.filter(r -> r.equals(linkedTestRev)).collect(Collectors.toList());
					debugPath = (String) this.getParameters().get("debugPath");
					debugContent = (String) this.getParameters().get("debugContent");
				}
				
				SZZImplementationType szzType = (SZZImplementationType) this.getParameters().get("szzType");		
				AnnotationGraphService worker = null;
				switch (szzType) {
					case RASZZ:
						System.out.println("Running RASZZ");
						worker = new AnnotationGraphServiceRaSZZ(repository, szzDAO, projects[j], 
								linkedRevs, repoUrl, debugPath, debugContent, szzType);
						break;
					case MASZZ:
						worker = new AnnotationGraphServiceMaSZZ(repository, szzDAO, projects[j], 
								linkedRevs, repoUrl, debugPath, debugContent, szzType);
						break;									
				}
				worker.run();
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		HibernateUtil.shutdown();
	}
}
