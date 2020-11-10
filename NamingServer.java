package naming;

import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import rmi.*;
import common.*;
import storage.*;


public class NamingServer implements Service, Registration {
	Skeleton<Service> servSkeleton;
	Skeleton<Registration> regSkeleton;
	Node dirTree;
	public ArrayList<Command> commandStubList;
	public HashMap<Command, Storage> commandStorageMap;
	public HashMap<Storage, Command> storageCommandMap;
	public HashMap<Path, Set<Storage>> pathStorageSetMap;

	public NamingServer() {
	InetSocketAddress serviceAdd = new InetSocketAddress(
				NamingStubs.SERVICE_PORT);
		InetSocketAddress regisAdd = new InetSocketAddress(
				NamingStubs.REGISTRATION_PORT);

	this.dirTree = new Node("/", new Path());
	this.commandStubList = new ArrayList<Command>();
		this.commandStorageMap = new HashMap<Command, Storage>();
		this.pathStorageSetMap = new HashMap<Path, Set<Storage>>();
		this.storageCommandMap = new HashMap<Storage, Command>();

		servSkeleton = new Skeleton<Service>(Service.class, this, serviceAdd);
		regSkeleton = new Skeleton<Registration>(Registration.class, this,
				regisAdd);	}


	public synchronized void start() throws RMIException {
		try {
			this.servSkeleton.start();
			this.regSkeleton.start();
		} catch (Exception e) {
		}
	}

	public void stop() {
		this.servSkeleton.stop();
		this.regSkeleton.stop();
		stopped(null);
	}


	protected void stopped(Throwable cause) {
	}

	
	 @Override
	public boolean isDirectory(Path path) throws FileNotFoundException {
		if (path.isRoot()) {
			return true;
		}	return dirTree.extract(path).isDirectory();
	}

	@Override
	public String[] list(Path directory) throws FileNotFoundException {
		if (!isDirectory(directory)) {
			throw new FileNotFoundException(
					"Wrong directory");
		}

		ArrayList<DirTree> f;

		if (directory.isRoot()) {
			f = this.dirTree.files;
		} else {
		}

		String[] dummyArray = new String[0];
		ArrayList<String> retArray = new ArrayList<String>();

		for (DirTree t : f) {
			retArray.add(t.getName());
		}

		return retArray.toArray(dummyArray);
	}

	@Override
	public boolean createFile(Path file) throws RMIException,
			FileNotFoundException {
		if (file.isRoot()) {
			return false;
		}
		Path actualPath = file.parent();
		if (!isDirectory(file.parent())) {
			throw new FileNotFoundException(
					"not a parent directory");
		}

		ArrayList<DirTree> f;

		if (actualPath.isRoot()) {
			f = this.dirTree.files;
		} else {
		}

		for (DirTree t : f) {
			if (t.getName().equals(file.last())) {
				return false;
			}
		}

		if (actualPath.isRoot()) {
			Path newPath = new Path(this.dirTree.getPath(), file.last());
			Random randomGenerator = new Random();
			int index = randomGenerator.nextInt(commandStubList.size());
			Command randomCommand = commandStubList.get(index);
			Storage randomStorage = this.commandStorageMap.get(randomCommand);

	
			randomCommand.create(file);
		} else {
			Path newPath = new Path(n.getPath(), file.last());
			Random randomGenerator = new Random();
			int index = randomGenerator.nextInt(commandStubList.size());

			Command randomCommand = commandStubList.get(index);
			Storage randomStorage = this.commandStorageMap.get(randomCommand);

			n.files.add(new EndNode(file.last(), newPath, randomStorage,
					randomCommand));

			randomCommand.create(file);
		}

		return true;
	}

	@Override
	public boolean createDirectory(Path directory)
			throws FileNotFoundException, RMIException {
		if (directory.isRoot()) {
			return false;
		}
		Path actualPath = directory.parent();
		if (!isDirectory(directory.parent())) {
			throw new FileNotFoundException(
					"not in  a directory");
		}

		ArrayList<DirTree> f;

		if (actualPath.isRoot()) {
			f = this.dirTree.files;
		} else {
		}

		for (DirTree t : f) {
			if (t.getName().equals(directory.last())) {
				return false;
			}
		}

		if (actualPath.isRoot()) {
			Path newPath = new Path(this.dirTree.getPath(), directory.last());

		} else {
			Path newPath = new Path(n.getPath(), directory.last());

		}

		return true;
	}



	@Override
	public Storage getStorage(Path file) throws FileNotFoundException {
		if (this.dirTree.extract(file).isDirectory()) {
			throw new FileNotFoundException("Path referred to a directory!");
		}
		return ((EndNode) this.dirTree.extract(file)).getStorageStub();
	}

	@Override
	public Path[] register(Storage client_stub, Command command_stub,
			Path[] files) {
		if (client_stub == null || command_stub == null || files == null) {
			throw new NullPointerException("Null Argument given!");
		}

		if (this.commandStubList.contains(command_stub)) {
			throw new IllegalStateException("Command Stub already registered!");
		}

		this.commandStubList.add(command_stub);
		this.commandStorageMap.put(command_stub, client_stub);
		this.storageCommandMap.put(client_stub, command_stub);

		ArrayList<Path> duplicatePaths = new ArrayList<Path>();
		Path[] dummyArray = new Path[0];

		for (Path p : files) {
			boolean created = dirTree.addRegistration(p, command_stub,
					client_stub);
			if (created == false) {
				duplicatePaths.add(p);
			} else {
				if (pathStorageSetMap.containsKey(p)) {
					pathStorageSetMap.get(p).add(client_stub);
				} else {
					Set<Storage> ss = new HashSet<Storage>();
					ss.add(client_stub);
					pathStorageSetMap.put(p, ss);
				}
			}

		}

		return duplicatePaths.toArray(dummyArray);
	}

	@Override
	public void lock(Path path, boolean exclusive) throws RMIException,
			FileNotFoundException {
		if (path == null) {
			throw new NullPointerException("Null Argument given!");
		}

		this.dirTree.extract(path);


		Tree currNode = this.dirTree;
		String pathAcc = "";
		int readers;
		for (String pComp : path.pComps) {
	
			pathAcc += "/" + pComp;
			Path currPath = new Path(pathAcc);
			currNode = this.dirTree.extract(currPath);
		}


	}

	@Override
	public void unlock(Path path, boolean exclusive) throws RMIException {
				if (path == null) {
			throw new NullPointerException("Null Argument given!");
		}

		try {
			this.dirTree.extract(path);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File not Found!!");
		}

		Set<Storage> storageSet = pathStorageSetMap.get(path);
		Path fileCopy = null;
		Storage chosenStorage = null;

		Tree currNode = this.dirTree;
		String pathAcc = "";
		int readers;

		for (String pComp : path.pComps) {


			pathAcc += "/" + pComp;
			Path currPath = new Path(pathAcc);

			try {
				currNode = this.dirTree.extract(currPath);
			} catch (FileNotFoundException e) {
				e.printStackTrace();  }}

		if (exclusive && storageSet != null) {

			chosenStorage = storageSet.iterator().next();
			
			storageSet.remove(chosenStorage);

			for (Storage s : storageSet) {
				Command cmd = this.storageCommandMap.get(s);
				cmd.delete(path);
			}

			if (chosenStorage != null) {
				Set<Storage> updatedStorage = new HashSet<Storage>();
				updatedStorage.add(chosenStorage);
				pathStorageSetMap.put(path, updatedStorage);
			} else {
				System.out.println("Chosen Storage is null!!");
			}}}}
