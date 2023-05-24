package com.thefirstlineofcode.granite.pack.cluster.mgtnode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Packer {
	private static final String FOLDER_UNZIP_TMP = "unzip-tmp";
	
	private Options options;
	
	public Packer(Options options) {
		this.options = options;
	}
	
	public void pack() {
		rebuildGraniteServer();
		
		File dependenciesDir = new File(options.getTargetDirPath(), "dependencies");
		refreshAppnodeDependencies(dependenciesDir);
		
		File artifactFile = copyMgtnodeArtifact();
		
		File unzipFolder = unzipMgtnodeArtifact(artifactFile);
		
		copyAppnodeDependenciesToRepository();
		
		rezipMgtnodeArtifact(unzipFolder, artifactFile);
	}

	private void refreshAppnodeDependencies(File dependenciesDir) {
		deleteAppnodeDependencies(dependenciesDir);
		recopyAppnodeDependencies();
	}
	
	private void rezipMgtnodeArtifact(File unzipFolder, File artifactFile) {
		if (artifactFile.exists()) {
			try {
				Files.delete(artifactFile.toPath());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Artifact file %s existed and couldn't delete it.", artifactFile.getPath()), e);
			}
		}
		
		try {
			ZipUtils.zip(unzipFolder, artifactFile);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Can't zip %s to file %s.",
					unzipFolder.getPath(), artifactFile.getPath()), e);
		}
		
		try {
			deleteFileRecursively(unzipFolder);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Can't delete temporary directory %s.", unzipFolder.getPath()), e);
		}
	}

	private File unzipMgtnodeArtifact(File artifactFile) {
		File unzipFolder = new File(options.getTargetDirPath(), FOLDER_UNZIP_TMP);
		if (unzipFolder.exists()) {
			try {
				deleteFileRecursively(unzipFolder);
			} catch (IOException e) {
				throw new RuntimeException(
						String.format("Temporary directory[%s] for unzip has existed and can't delete it.",
								unzipFolder.getPath()), e);
			}
		}
		
		unzipFolder.mkdirs();
		
		try {
			ZipUtils.unzip(artifactFile, unzipFolder);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Can't unzip file %s.", artifactFile), e);
		}
		
		artifactFile.delete();
		
		return unzipFolder;
	}
	
	public void deleteFileRecursively(File file) throws IOException {
		if (!file.isDirectory()) {
			Files.delete(file.toPath());
		} else {
			for (File subFile : file.listFiles()) {
				deleteFileRecursively(subFile);
			}
			
			file.delete();
		}
	}
	
	private void copyAppnodeDependenciesToRepository() {
		File repositoryDir = new File(options.getTargetDirPath(), FOLDER_UNZIP_TMP + "/" + options.getAppName() + "/repository");
		if (!repositoryDir.exists()) {
			repositoryDir.mkdirs();
		}
		
		copyDependenciesToRepository(repositoryDir);
	}

	private void copyDependenciesToRepository(File repositoryDir) {
		File dependenciesDir = new File(options.getTargetDirPath(), "dependencies");
		for (File dependency : dependenciesDir.listFiles()) {
			try {
				Files.copy(dependency.toPath(), new File(repositoryDir, dependency.getName()).toPath(),
						StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new RuntimeException("Can't copy a dependency to repository.", e);
			}
		}
	}

	private void rebuildGraniteServer() {
		File serverDir = new File(options.getGraniteProjectDirPath(), "server");
		if (!serverDir.exists() || !serverDir.isDirectory())
			throw new RuntimeException("Can't determine granite server directory.");
		
		PackUtils.runMvn(serverDir, options.isOffline(), "clean", "install");
	}

	private File copyMgtnodeArtifact() {
		File deployClusterDir = new File(options.getGraniteProjectDirPath(), "cluster");
		File nodeProjectDir = new File(deployClusterDir, "node");
		File mgtnodeProjectDir = new File(nodeProjectDir, "mgtnode");
		PackUtils.runMvn(mgtnodeProjectDir, options.isOffline(), "clean", "package");
		
		File mgtnodeProjectTargetDir = new File(mgtnodeProjectDir, "target");
		File mgtnodeArtifactSourceFile = new File(mgtnodeProjectTargetDir, options.getAppName() + ".zip");
		if (!mgtnodeArtifactSourceFile.exists()) {
			throw new RuntimeException("MgtNode zip file not found.");
		}
		
		
		try {
			File mgtnodeArtifactTargetFile = new File(options.getTargetDirPath(),
					mgtnodeArtifactSourceFile.getName());
			Files.copy(mgtnodeArtifactSourceFile.toPath(), mgtnodeArtifactTargetFile.toPath(),
					StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			
			return mgtnodeArtifactTargetFile;
		} catch (IOException e) {
			throw new RuntimeException("Can't copy mgtnode zip file.", e);
		}
	}

	private void recopyAppnodeDependencies() {
		PackUtils.runMvn(new File(options.getProjectDirPath()), options.isOffline(), "-f", "repository-pom.xml", "dependency:copy-dependencies");
		if (options.isCommerical()) {			
			PackUtils.runMvn(new File(options.getProjectDirPath()), options.isOffline(), "-f", "gem-pom.xml", "dependency:copy-dependencies");
		}
	}
	
	private void deleteAppnodeDependencies(File dependenciesDir) {
		if (dependenciesDir.exists()) {
			System.out.println(String.format("Dependencies directory %s has existed. delete it...", dependenciesDir.getPath()));
			
			File[] files = dependenciesDir.listFiles();
			
			for (File file : files) {
				try {
					Files.delete(file.toPath());
				} catch (IOException e) {
					throw new RuntimeException(String.format("Can't delete file %s.", file.getPath()), e);
				}
			}
			
			try {
				Files.delete(dependenciesDir.toPath());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Can't delete dependencies directory %s. Maybe you should delete it manually.",
						dependenciesDir.getPath()), e);
			}
		}
	}
	
}
