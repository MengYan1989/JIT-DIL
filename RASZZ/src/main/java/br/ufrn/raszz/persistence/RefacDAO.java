package br.ufrn.raszz.persistence;

import java.util.List;

import br.ufrn.raszz.model.RefCaller;
import br.ufrn.raszz.model.RefElement;

public abstract class RefacDAO extends AbstractDAO {
	
	public abstract void saveRefDiffResults(RefElement ref, String revisionType);

	public abstract void saveCallersRefDiffResults(RefCaller caller, String revisionType);
	
	public abstract void insertRefDiffRevisionsProcessed(String project, String revision);
	
}
