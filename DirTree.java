package naming;

import common.Path;

public abstract class DirTree {
	
	private String name;
	private Path ph;
	public int noReaders;
	public int noReads;
	
	public DirTree(String name, Path p) {
		this.name = name;
		this.ph = p;
		this.noReaders = 0;
		this.noReads = 0;
	
	}
	
	public String getName() {
		return this.name;
	}

	public Path getPath() {
		return this.ph;
	}
	
	public abstract boolean isDirectory();

}
