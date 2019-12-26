package refdiff.core.rm2.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SDEntity implements Comparable<SDEntity> {

	private int id;
	protected final SDModel.Snapshot snapshot;
	protected final String fullName;
	protected final EntityKey key;
	protected final SDContainerEntity container;
	protected final List<SDEntity> children;
	
	protected long startposition;
	protected long endposition;
	protected long firststatementline;
	private String content;
	private String firststatement;
	private long callerline;
	
	private MembersRepresentation members = null;
	
	public SDEntity(SDModel.Snapshot snapshot, int id, String fullName, EntityKey key, SDContainerEntity container) {
		this.snapshot = snapshot;
		this.id = id;
		this.fullName = fullName;
		this.key = key;
		this.container = container;
		this.children = new ArrayList<SDEntity>();
		if (container != null) {
		    this.container.children.add(this);
		}
	}
	
	public int getId() {
		return id;
	}
	
	int setId(int id) {
		return this.id = id;
	}
	
	public String fullName() {
		return fullName;
	}

	public EntityKey key() {
	  return key;
	}
	
	public EntityKey key(SDEntity parent) {
	  return new EntityKey(parent.key() + getNameSeparator() + simpleName());
	}

//	public String fullName(SDEntity parent) {
//		return parent.fullName() + getNameSeparator() + simpleName();
//	}
	
	protected abstract String getNameSeparator();

	public abstract String simpleName();
	
	public SDContainerEntity container() {
		return container;
	}
	
	public abstract boolean isTestCode();
	
	public abstract String getVerboseFullLine();
	
	@Override
	public int hashCode() {
//		return fullName.hashCode();
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SDEntity) {
		    SDEntity e = (SDEntity) obj;
			return id == e.id;
			//return e.fullName.equals(fullName);
		}
		return false;
	}

	@Override
	public String toString() {
	  if (fullName.lastIndexOf('/') != -1) {
	    return fullName.substring(fullName.lastIndexOf('/') + 1);
	  }
		return fullName;
	}

	public Iterable<SDEntity> children() {
		return children;
	}
	
	@Override
	public int compareTo(SDEntity o) {
		return fullName.compareTo(o.fullName);
	}

	public SourceRepresentation sourceCode() {
	    throw new UnsupportedOperationException();
	}

	public long simpleNameHash() {
	    long h = 0;
	    String simpleName = this.simpleName();
        for (int i = 0; i < simpleName.length(); i++) {
            h = 31 * h + simpleName.charAt(i);
        }
        return h;
	}
	
	public MembersRepresentation membersRepresentation() {
	    if (this.members == null) {
	        Map<Long, String> debug = new HashMap<Long, String>();
	        long[] hashes = new long[this.children.size()];
	        for (int i = 0; i < hashes.length; i++) {
	            hashes[i] = this.children.get(i).simpleNameHash();
	            debug.put(hashes[i], this.children.get(i).simpleName());
	        }
	        Arrays.sort(hashes);
	        this.members = new MembersRepresentation(hashes, debug);
	    }
	    return this.members;
	}

	public void addReferencedBy(SDMethod method, long line) {
	    throw new UnsupportedOperationException();
	}

	public void addReferencedBy(SDType type, long line) {
	    throw new UnsupportedOperationException();
	}
	
	public void addReference(SDEntity entity) {
	    throw new UnsupportedOperationException();
	}
	
	public boolean isAnonymous() {
        return false;
    }
	
	public String pathfile (){
		String result = key().toString();
		//e.g.: /incubator/activemq/trunk/activemq-core/src/main/java/org.apache.activemq.kaha.impl.StoreByteArrayInputStream#position()
	    result = result.split("#")[0];
		result = result.replace('.', '/');
		result += ".java";		
		return result;
	}
	
	public abstract int countCallers();
	
	public String getElementType(){
		String name = this.getClass().getName();
		String[] tokens = name.split("\\.");
		return tokens[tokens.length-1].substring(2);
	}

	public long getStartposition() {
		return startposition;
	}

	public void setStartLine(long startposition) {
		this.startposition = startposition;
	}

	public long getEndposition() {
		return endposition;
	}

	public void setEndLine(long endposition) {
		this.endposition = endposition;
	}

	public long getFirstpositionstatement() {
		return firststatementline;
	}

	public void setFirstpositionstatement(long firstpositionstatement) {
		this.firststatementline = firstpositionstatement;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getFirststatement() {
		return firststatement;
	}

	public void setFirststatement(String firststatement) {
		this.firststatement = firststatement;
	}

	public long getCallerline() {
		return callerline;
	}

	public void setCallerline(long callerline) {
		this.callerline = callerline;
	}	
	
}
