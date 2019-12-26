package br.ufrn.raszz.persistence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.ufrn.raszz.model.szz.SimpleBugIntroducingChange;
import org.hibernate.SQLQuery;

import br.ufrn.raszz.model.RefElement;
import br.ufrn.raszz.model.Refac;
import br.ufrn.raszz.model.SZZImplementationType;
import br.ufrn.raszz.model.szz.BugIntroducingCode;

public class SzzDAOImpl extends SzzDAO {
	
	@Override
	public synchronized List<String> getGitLinkedRevision(String project) {
		String sql = "select distinct fix_revision from linkedissuegit lgit " +
			"where project like :project";

		System.out.println(sql);
		List<String> revisions = new ArrayList<String>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();
		return revisions;
	}


	public synchronized List<String> getAllRevisionProcessed(String project){
		String sql = "select lastrevisionprocessed from szz_project_lastrevisionprocessed " +
			"where project = :project";
		List<String> revisionsConverted = new ArrayList<String>();
		List<Object[]> revisions = new ArrayList<Object[]>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();
		for (Object revision : revisions) {
			String revisionconverted = revision.toString();
			revisionsConverted.add(revisionconverted);
		}
		return revisionsConverted;
		
	}
	
	@Override
	public synchronized Map<String,String> getAllRefDiffRevisionsProcessed(String project) {
		String sql = "select project, revision from szz_refac_revisionprocessed "
				+ "where project = :project and tool = 'refdiff'";
		Map<String,String> revisionsConverted = new HashMap<String,String>();
		List<Object[]> revisions = new ArrayList<Object[]>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();
		for (Object[] revision : revisions) {
			if (revision[1] != null) {
				String projectconverted = revision[0].toString();
				String revisionconverted = revision[1].toString();
				revisionsConverted.put(revisionconverted, projectconverted);
			}
		}
		return revisionsConverted;
	}
	
	@Override
	public synchronized void insertProjectRevisionsProcessed(String project, String revision){
		String sql = "insert into szz_project_lastrevisionprocessed values (:param1,:param2)";
		executeSQLWithParams(sql,project, revision);
	}

	@Override
	public synchronized void insertBugIntroducingCode(BugIntroducingCode bicode, SZZImplementationType szzType){
		switch (szzType) {
		case RASZZ:
			insertRABugIntroducingCode(bicode);
			break;
		case MASZZ:
			insertMABugIntroducingCode(bicode);
			break;									
		}		
	}
		
	private synchronized void insertRABugIntroducingCode(BugIntroducingCode bicode){
		String sql = "INSERT INTO bicraszzold (linenumber, path, content, revision, "
				+ "fixrevision, project, szz_date, mergerev, branchrev, "
				+ "changeproperty, missed, furtherback, diffjmessage, diffjlocation, adjustmentindex, "
				+ "indexposrefac, indexchangepath, isrefac, indexfurtherback, "
				+ "startrevision, startpath, startlinenumber, startcontent) "
				+ "values (:param1,:param2, :param3, :param4, :param5, :param6, :param7, :param8,"
				+ " :param9, :param10, :param11, :param12,:param13, :param14, :param15, :param16, "
				+ ":param17, :param18, :param19, :param20, :param21, "
				+ ":param22, :param23)";
		executeSQLWithParams(sql,bicode.getLinenumber(), bicode.getPath(), bicode.getContent(),
				bicode.getRevision(), bicode.getFixRevision(), bicode.getProject(), 
				bicode.getSzzDate(),
				bicode.getMergerev(),bicode.getBranchrev(),bicode.getChangeproperty(),
				bicode.getMissed(),bicode.getFurtherback(), bicode.getDiffjmessage(), 
				bicode.getDiffjlocation(), bicode.getAdjustmentIndex(), 
				bicode.getIndexPosRefac(), bicode.getIndexChangePath(),
				bicode.isIsrefac(), bicode.getIndexFurtherBack(),
				bicode.getStartRevision(), bicode.getStartPath(),
				bicode.getStartlinenumber(), bicode.getStartContent());	       
	}
	
	private synchronized void insertMABugIntroducingCode(BugIntroducingCode bicode){
		String sql = "INSERT INTO bicmaszztest(linenumber, path, content, revision, "
				+ "fixrevision, project, szz_date, mergerev, branchrev, "
				+ "changeproperty, missed, furtherback, diffjmessage, diffjlocation, indexfurtherback, "
				+ "startrevision, startpath, startlinenumber, startcontent) "
				+ "values (:param1,:param2, :param3, :param4, :param5, :param6, :param7, :param8,"
				+ " :param9, :param10, :param11, :param12,:param13, :param14, :param15, :param16,"
				+ " :param17, :param18, :param19)";
		executeSQLWithParams(sql,bicode.getLinenumber(), bicode.getPath(), bicode.getContent(),
				bicode.getRevision(), bicode.getFixRevision(), bicode.getProject(), 
				bicode.getSzzDate(),
				bicode.getMergerev(),bicode.getBranchrev(),bicode.getChangeproperty(),
				bicode.getMissed(),bicode.getFurtherback(), bicode.getDiffjmessage(), 
				bicode.getDiffjlocation(), bicode.getIndexFurtherBack(),
				bicode.getStartRevision(), bicode.getStartPath(),
				bicode.getStartlinenumber(), bicode.getStartContent());	       
	}


	@Override
	public synchronized boolean hasRefacFix(String path, String fixrevision, int linenumber, int adjustmentindex, String content) {
		String sql = "SELECT count(*) FROM (SELECT DISTINCT * " +
						"FROM refdiffresult ref " +
						"WHERE (:fixrevision = ref.revision AND :path = ref.beforepathfile) " +
							"AND " +
						      "((refactoringtype IN ('EXTRACT_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('EXTRACT_INTERFACE') AND :content like concat('%class%',REF.beforesimplename,'%')) OR " +
							"(refactoringtype IN ('EXTRACT_SUPERCLASS') AND :content like concat('%class%',REF.beforesimplename,'%')) OR " +
							"(refactoringtype IN ('INLINE_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('MOVE_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('MOVE_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('MOVE_CLASS')) OR " + //AND (:content like concat('%class%',REF.beforesimplename,'%') OR :content like concat('%interface%',REF.beforesimplename,'%') OR :content like concat('%enum%',REF.beforesimplename,'%'))
							"(refactoringtype IN ('PULL_UP_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('PULL_UP_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('PUSH_DOWN_ATTRIBUTE') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('PUSH_DOWN_OPERATION') AND (:linenumber + :adjustmentindex) >= REF.beforestartline AND :linenumber <= REF.beforeendline) OR " +
							"(refactoringtype IN ('RENAME_METHOD') AND :linenumber >= REF.beforestartline AND :linenumber < REF.beforestartscope) OR " +
							"(refactoringtype IN ('RENAME_CLASS') AND (:linenumber >= beforestartline " +
								"AND (:linenumber <= REF.beforestartscope OR (:linenumber = REF.beforestartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS') AND (REF.beforenestinglevel = 0 AND :linenumber >= 1 " +
								"AND (:linenumber <= REF.beforestartscope OR (:linenumber = REF.beforestartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS') AND (REF.beforenestinglevel >= 0 AND :linenumber >=beforestartline and (:linenumber <= REF.beforestartscope OR (:linenumber = REF.beforestartscope+1 AND (:content like '%class%' or :content like '%interface%' or :content like '%enum%'))) " +
								")) OR " +
							"(refactoringtype IN ('MOVE_RENAME_CLASS', 'RENAME_CLASS') AND (:content like concat('%class%',REF.beforesimplename,'%') OR :content like concat('%interface%',REF.beforesimplename,'%') OR :content like concat('%enum%',REF.beforesimplename,'%')) " +
								") " +
							   " )) T1 UNION ALL "+
							   "SELECT count(*) FROM (SELECT DISTINCT * " +
							   "FROM callerrefdiff ref " +
							   "WHERE (:fixrevision = ref.revision " +
							   	"AND ref.type = 'before' " +
							   	"AND ref.revisiontype like 'ERROR' " + //ERROR PROPOSITAL
							   	"AND ref.tool = 'refdiff' " +
							   	"AND :path = ref.callerpath " +
							   	"AND :linenumber = ref.callerline)) T2";

		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("path", path);
		query.setParameter("fixrevision", fixrevision);
		query.setParameter("linenumber", linenumber);
		query.setParameter("adjustmentindex", adjustmentindex);
		query.setParameter("content", content);

		List<BigInteger> countref = query.list();
		if (countref.get(0).intValue() == 0 && countref.get(1).intValue() == 0 )
		//List<Object> countref = query.list();
		//if (countref.get(0).toString().equals(0) && countref.get(1).toString().equals(0))
			return false;
		else
			return true;
	}

	
	private List<RefElement> preencher(List<Object[]> results, String project){
		List<RefElement> refacs = new ArrayList<RefElement>();
		for (Object[] result : results) {
			RefElement refac = new RefElement(result[0].toString(), project);
			refac.setRefactoringtype(result[1].toString());
			refac.setEntitybefore(result[2].toString());
			refac.setEntityafter(result[3].toString());
			refac.setElementtype(result[4].toString());
			refac.setAfterstartline(Long.parseLong(result[5].toString()));
			refac.setAfterendline(Long.parseLong(result[6].toString()));
			refac.setAfterstartscope(Long.parseLong(result[7].toString()));
			refac.setAftersimpleName(result[8].toString());
			refac.setAfterpathfile(result[9].toString());
			refac.setAfternestingLevel(Integer.parseInt(result[10].toString()));
			refac.setBeforepathfile(result[11].toString());
			refac.setBeforestarscope(Integer.parseInt(result[12].toString()));
			refac.setBeforestartline(Integer.parseInt(result[13].toString()));
			refac.setBeforeendline(Integer.parseInt(result[14].toString()));
			refacs.add(refac);
		}
		return refacs;
	}
	
	@Override
	public synchronized List<RefElement> getRefacBic(String project) {
		String sql = "SELECT distinct revision, refactoringtype, entitybefore, entityafter, elementtype, afterstartline, afterendline, afterstartscope,"
				+ " aftersimplename, afterpathfile, afternestinglevel, beforepathfile, beforestartscope, beforestartline, beforeendline " +
						"FROM refdiffresult ref " +
						"WHERE (:project = ref.project " + 
						//	"AND ref.revisiontype like 'new%' " + 
						 "AND ref.tool = 'refdiff' " +
						      ") " /*+						
					  "UNION ALL "+
						 "SELECT distinct cal.revision, cal.refactoringtype, ref.entitybefore, cal.entityafter, 'CALLER', cal.callerstartline, cal.callerendline, cal.callerline, cal.simplename, cal.callerpath, cal.nestinglevel, '', 0 " + 
						   "FROM callerrefdiff cal inner join refdiffresult ref on (ref.summary = cal.summary)" + 
							   "WHERE (:project = cal.project " + 
							   	"AND cal.type = 'after' " +
							   	"AND cal.revisiontype like 'new%')"*/;
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		List<Object[]> results = query.list();
		return preencher(results, project);			
	}

	public synchronized Map<String, List<SimpleBugIntroducingChange>> getBugIntroducingChangeFromRASZZ(String project){
		String sql = "SELECT linenumber, path, content, revision FROM bicraszzold WHERE" +
				":project=project";
		List<Object[]> revisions = new ArrayList<Object[]>();
		SQLQuery query = currentSession.createSQLQuery(sql);
		query.setParameter("project", project);
		revisions = query.list();

		Map<String, List<SimpleBugIntroducingChange>> result = new HashMap<>();
		for (Object[] revision : revisions) {
			if (revision[3] != null) {
				long lineNumber = Long.valueOf(revision[0].toString());
				String path = revision[1].toString();
				String content = revision[2].toString();
				String commitId = revision[3].toString();
				SimpleBugIntroducingChange sbic = new SimpleBugIntroducingChange();
				sbic.setCommitId(commitId);
				sbic.setContent(content);
				sbic.setLineNumber(lineNumber);
				sbic.setProject(project);
				sbic.setPath(path);
				if (result.get(commitId) != null)
					result.get(commitId).add(sbic);
				else{
					result.put(commitId, new ArrayList<>());
					result.get(commitId).add(sbic);
				}
			}
		}
		return result;
	}

}
