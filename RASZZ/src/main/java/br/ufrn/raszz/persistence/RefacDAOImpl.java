package br.ufrn.raszz.persistence;


import br.ufrn.raszz.model.RefCaller;
import br.ufrn.raszz.model.RefElement;

public class RefacDAOImpl extends RefacDAO {

	@Override
	public synchronized void saveRefDiffResults(RefElement ref, String revisionType) {
		String sql = "INSERT INTO refdiffresult (revision, project,summary,"
				+ "refactoringtype, entityafter, entitybefore, elementtype,"
				+ "callers, afterstartline, afterendline, afterpathfile,"
				+ "beforestartline, beforeendline, beforepathfile, "
				+ "afterstartscope, aftersimpleName, aftercontent, afternestinglevel, revisiontype, "
				+ "beforestartscope, beforesimpleName, beforecontent, beforenestinglevel, tool) "
				+ "values (:param1,:param2, :param3, :param4, :param5, :param6,"
				+ ":param7,:param8, :param9, :param10, :param11, :param12, "
				+ ":param13, :param14, :param15, :param16,:param17, :param18, :param19,"
				+ ":param20,:param21, :param22, :param23, 'refdiff')";
		executeSQLWithParams(sql, ref.getRevision(), ref.getProject(), ref.getSummary(), ref.getRefactoringtype(),
				ref.getEntityafter(), ref.getEntitybefore(), ref.getElementtype(), ref.getCallers(),
				ref.getAfterstartline(), ref.getAfterendline(), ref.getAfterpathfile(), ref.getBeforestartline(),
				ref.getBeforeendline(), ref.getBeforepathfile(), ref.getAfterstartscope(),
				ref.getAftersimplename(), ref.getContent(), ref.getAfternestingLevel(), revisionType,
				ref.getBeforestarscope(), ref.getBeforesimpleName(), ref.getBeforecontent(), ref.getBeforenestingLevel());
	}

	@Override
	public synchronized void saveCallersRefDiffResults(RefCaller caller, String revisionType) {
		String sql = "INSERT INTO callerrefdiff (revision, project, summary,"
				+ "entityafter, callermethod, callerstartline,callerendline, "
				+ "callerpath, refactoringtype, callerline, simplename, "
				+ "nestinglevel, revisiontype, type, tool) "
				+ "values (:param1,:param2, :param3, :param4, :param5, "
				+ ":param6,:param7,:param8,:param9, :param10, :param11, "
				+ ":param12, :param13, :param14, 'refdiff')";
		executeSQLWithParams(sql, caller.getRevision(), caller.getProject(), caller.getSummary(), caller.getEntityafter(),
				caller.getCallermethod(), caller.getCallerstartline(), caller.getCallerendline(), caller.getCallerpath(), 
				caller.getRefactoringtype(), caller.getCallerline(), caller.getSimplename(), caller.getNestingLevel(), 
				revisionType, caller.getType());
	}
	
	@Override
	public synchronized void insertRefDiffRevisionsProcessed(String project, String revision){
		String sql = "insert into szz_refac_revisionprocessed (project, revision, tool) values (:param1,:param2, 'refdiff')";
		executeSQLWithParams(sql,project,revision);
	}
}
