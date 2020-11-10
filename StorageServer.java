package storage;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import common.*;
import rmi.*;
import naming.*;


public class StorageServer implements Storage, Command
{
	public File root;
	Skeleton<Storage> storageSkeleton;
	Skeleton<Command> commandSkeleton;
	
  
    public StorageServer(File root)
    {
    	if (root == null) {
    		throw new NullPointerException("Null argument passed in!");
    	}
    	
    	this.root = root;
    	storageSkeleton = new Skeleton<Storage>(Storage.class, this);
    	commandSkeleton = new Skeleton<Command>(Command.class,this);
    	
    }

    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
    	// Error checking
    	if(!root.exists() || root.isFile()) {
    		throw new FileNotFoundException("Directory with which the server was"
                    + "created does not exist or is in fact a file");
    	}
    	
    	storageSkeleton.start();
    	commandSkeleton.start();
    	
    	Storage storageStub = (Storage) Stub.create(Storage.class, storageSkeleton, hostname);
    	Command commandStub = (Command) Stub.create(Command.class, commandSkeleton, hostname);
    	
    	Path[] dupFiles = naming_server.register(storageStub, commandStub, Path.list(root));
    	
    	for (Path p : dupFiles) {
    		File currentFile = p.toFile(root);
    		File parentFile = new File(currentFile.getParent());
    		currentFile.delete();
    		
    		while(!parentFile.equals(root)) {
    			if (parentFile.list().length == 0) {
        			parentFile.delete();
        			parentFile =  new File(parentFile.getParent());
        		} else {
        			break;
        		}
    		}
    	}
    	
    }

    
    public void stop()
    {
        storageSkeleton.stop();
        commandSkeleton.stop();
        this.stopped(null);
    }

    protected void stopped(Throwable cause)
    {
    }

    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File f = file.toFile(root);
        if (f.isDirectory() || !f.exists()) {
        	throw new FileNotFoundException("File cannot be found or is a directory!");
        }
        return f.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
    	File f = file.toFile(root);
    	
    	// Error Checking
        if (f.isDirectory() || !f.exists()) {
        	throw new FileNotFoundException("File cannot be found or is a directory!");
        }
        
        if (offset < 0 || length < 0 || length + offset > f.length()) {
        	throw new IndexOutOfBoundsException("Offset and length are out of bounds "
        			+ "given the length of the file or they are negative!");
        }
        
        InputStream fin = new FileInputStream(f);
    	byte[] fileContent = new byte[length];
    	fin.read(fileContent, ((int) offset), length);
    	
    	if (fin != null) {    		
    		fin.close();
    	}
    	
    	return fileContent;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
    	File f = file.toFile(root);

    	// Error Checking
    	if (f.isDirectory() || !f.exists()) {
        	throw new FileNotFoundException("File cannot be found or is a directory!");
        }
    	
    	if (offset < 0) {
        	throw new IndexOutOfBoundsException("Offset is negative!");
        }
    	FileOutputStream fos = new FileOutputStream(f);
    	FileChannel ch = fos.getChannel();
    	ch.position(offset);
    	ch.write(ByteBuffer.wrap(data));
    	
    	if(fos != null) {    		
    		fos.close();
    	}
    	
    }

    @Override
    public synchronized boolean create(Path file)
    {
    	File f = file.toFile(root);
    	// Cannot create the root, so return false
        if (file.isRoot()) {
        	return false;
        }
        
        File parentFile = file.parent().toFile(root);

        if (!parentFile.isDirectory()) { 
        	parentFile.mkdirs();
        }
        
        // Create the file
        try {
			return f.createNewFile();
		} catch (IOException e) {
			return false;
		}
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        File f = path.toFile(root);
        
        // Cannot delete root so return false
        if (path.isRoot()) {
        	return false;
        }
        
        if (f.isFile()) {
        	return f.delete();
        } else {
        	File[] fileList = f.listFiles();
        	
        	if (fileList != null) {
        		for (File f1 : fileList) {
        			if(deleteDir(f1) == false){
        				return false;
        			}
            	}
        	}	
    		return f.delete();
        }
    }
    
    private boolean deleteDir(File f) {
    	File[] fileList = f.listFiles();
    	
    	if (fileList != null) {
    		for (File f1 : fileList) {
    			if(deleteDir(f1) == false){
    				return false;
    			}
        	}
    	}	
		return f.delete();
    }

	@Override
	public boolean copy(Path file, Storage server) throws RMIException,
			FileNotFoundException, IOException {
		File f = file.toFile(root);
		long fileSize = server.size(file);
		byte[] bytesToCopy;
		int readby = Integer.MAX_VALUE;
		
		if (f.exists()) {
			f.delete();
		}
		create(file);
		
		for(long offset = 0; offset < fileSize; offset+=readby) {
			readby = (int) Math.min(readby, fileSize-offset);
			bytesToCopy = server.read(file, offset, readby);
			write(file, offset, bytesToCopy);
		}
		
		return true;
	}
}
