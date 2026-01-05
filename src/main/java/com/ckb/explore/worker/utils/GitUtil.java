package com.ckb.explore.worker.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j

public class GitUtil {

    private static final String JSON_SUFFIX = ".json";


    public static List<File> listAllTargetJsonFiles(File dir) {
        List<File> jsonFiles = new ArrayList<>();
        if (!dir.isDirectory()) {
            return jsonFiles;
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                // 递归遍历子目录
                jsonFiles.addAll(listAllTargetJsonFiles(file));
            } else if (file.getName().toLowerCase().endsWith(JSON_SUFFIX)) {
                // 筛选 JSON 文件
                jsonFiles.add(file);
            }
        }
        return jsonFiles;
    }

    public static boolean isLocalRepoInited(String localPath) {
        File localRepoFile = new File(localPath);
        if (!localRepoFile.exists()) {
            return false;
        }
        File gitDir = new File(localPath + File.separator + ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    public static void initLocalEmptyRepo(String localPath) throws GitAPIException {
        File localRepoFile = new File(localPath);
        // 1. 本地目录不存在 → 克隆远程仓库
        if (!localRepoFile.exists()) {
            boolean dirCreated = localRepoFile.mkdirs();
            if (!dirCreated) {
                throw new RuntimeException("创建本地仓库目录失败：" + localPath);
            }
        }
        InitCommand initCommand = Git.init().setDirectory(localRepoFile);
        initCommand.call();


    }

    public static Git openLocalRepo(String localPath) throws IOException {
        File gitDir = new File(localPath + File.separator + ".git");
        if (!gitDir.exists()) {
            return null;
        }
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(localPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();
        return new Git(repository);

    }


    /**
     * 获取拉取前后的版本差异，返回变更文件列表
     */
    public static List<DiffEntry> getDiffEntries(Repository repository) throws IOException, GitAPIException {
        // 获取拉取前的最新提交（HEAD^）和拉取后的最新提交（HEAD）
        RevWalk revWalk = new RevWalk(repository);
        ObjectId head = repository.resolve("HEAD");
        ObjectId oldHead = repository.resolve("HEAD^"); // 上一个版本

        // 处理首次拉取的情况（无 HEAD^）
        if (oldHead == null) {
            oldHead = repository.resolve("HEAD");
        }

        RevCommit newCommit = revWalk.parseCommit(head);
        RevCommit oldCommit = revWalk.parseCommit(oldHead);

        // 构建两棵树的迭代器（对比版本）
        AbstractTreeIterator oldTreeIterator = getTreeIterator(repository, oldCommit);
        AbstractTreeIterator newTreeIterator = getTreeIterator(repository, newCommit);

        // 对比差异，获取变更文件
        DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        diffFormatter.setRepository(repository);
        List<DiffEntry> diffEntries = diffFormatter.scan(oldTreeIterator, newTreeIterator);

        revWalk.close();
        diffFormatter.close();
        return diffEntries;
    }


    public static AbstractTreeIterator getTreeIterator(Repository repository, RevCommit commit) throws IOException {
        RevWalk revWalk = new RevWalk(repository);
        RevTree tree = revWalk.parseTree(commit.getTree().getId());
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        ObjectReader reader = repository.newObjectReader();
        treeParser.reset(reader, tree.getId());
        revWalk.close();
        reader.close();
        return treeParser;
    }


    public static List<DiffEntry> filterJsonFiles(String path,List<DiffEntry> diffEntries) {
        return diffEntries.stream()
                // 只处理新增/修改，跳过删除
                .filter(entry -> entry.getChangeType() == DiffEntry.ChangeType.ADD ||
                        entry.getChangeType() == DiffEntry.ChangeType.MODIFY)
                // 只保留指定目录下的文件
                .filter(entry -> entry.getNewPath().contains(path + "/"))
                // 只保留JSON文件
                .filter(entry -> entry.getNewPath().endsWith(JSON_SUFFIX))
                .toList();
    }
}
