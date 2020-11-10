package naming;

import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import storage.Command;
import storage.Storage;
import common.Path;


public class Node extends DirTree {

	public ArrayList<DirTree> files;

	public Node(String name, Path p) {
		super(name, p);
		this.files = new ArrayList<DirTree>();
	}

	public DirTree extract(Path p) throws FileNotFoundException {
		Path dummyPath = new Path(p.pComps);
		return extractRec(dummyPath);
	}

	public DirTree extractRec(Path p) throws FileNotFoundException {
		
		if (p.pComps.size() == 0) {
			return this;
		}

		String firstComp = p.pComps.get(0);

		if (p.pComps.size() == 1) {
			for (Tree t : files) {
				if (t.getName().equals(firstComp)) {
					return t;
				}
			}
		} else {
			for (DirTree t : this.files) {
				if (t.getName().equals(firstComp) && t.isDirectory()) {
					p.pComps.remove(0);
					return ((Node) t).extractRec(p);
				} else if (t.getName().equals(firstComp) && !t.isDirectory()) {
					throw new FileNotFoundException("Path is incorrect!");
				}
			}
		}
		// Did not find file/directory
		throw new FileNotFoundException("Path does not refer to a file!");
	}

	public boolean addRegistration(Path p, Command commandStub,
			Storage storageStub) {
		if (p.isRoot()) {
			return true;
		}
		Path dummyPath = new Path(p.pComps);
		return addRec(dummyPath, commandStub, storageStub, new Path());
	}

	public boolean addRec(Path p, Command commandStub, Storage storageStub,
			Path pathAcc) {

		
		if (p.pComps.size() == 0) {
			return true;
		}

		// Else we get the first component and add it to the accumulated path
		String firstComp = p.pComps.get(0);
		pathAcc.pComps.add(firstComp);

		if (p.pComps.size() == 1) {
			for (DirTree t : files) {
				if (t.getName().equals(firstComp)) {
					return false;
				}
			}
			this.files.add(new EndNode(firstComp, pathAcc, storageStub,
					commandStub));
			return true;
		}

		// Search in the array for a leaf/node with name == to firstComp
		for (DirTree t : files) {
			// If it's a node then recurse
			if (t.getName().equals(firstComp) && t.isDirectory()) {
				// Remove the first component in the path components, add it
				// to the accumulated path and then recurse
				p.pComps.remove(0);
				return ((Node) t).addRec(p, commandStub, storageStub, pathAcc);
			}
			// If it's a file, return false immediately, this is a duplicate
			// file
			else if (t.getName().equals(firstComp) && !(t.isDirectory())) {
				return false;
			}
		}


		Node n = new Node(firstComp, pathAcc);
		this.files.add(n);
		p.pComps.remove(0);

		return n.addRec(p, commandStub, storageStub, pathAcc);
	}

	@Override
	public String toString() {
		String ret = "";
		System.out.println("This is: " + this.getName());

		for (DirTree t : files) {
			System.out.println(t.getName());
		}

		for (DirTree t : files) {
			if (t.isDirectory() == true) {
				ret += t.getName() + "/" + t.toString();
			} else {
				ret += t.getName() + "\n";
			}
		}
		return ret;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	public Set<Storage> removeDir() throws InterruptedException {
		Set<Storage> ss = new HashSet<Storage>();
		for (DirTree t : this.files) {
			if (!t.isDirectory()) {
				ss.add(((EndNode) t).getStorageStub());
			} else {
				Set<Storage> s1 = ((Node) t).removeDir();
				ss.addAll(s1);
			}
		}
		
		return ss;
	}

	public boolean removeLeaf(Path path) {
		String f = path.pComps.get(path.pComps.size()-1);
		for (DirTree t : files) {
			if (t.getName().equals(f)) {
				files.remove(t);
				return true;
			}
		}
		return false;}}
