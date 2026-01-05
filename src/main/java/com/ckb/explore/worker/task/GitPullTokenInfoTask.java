package com.ckb.explore.worker.task;
import com.ckb.explore.worker.entity.Script;
import com.ckb.explore.worker.mapper.ScriptMapper;
import com.ckb.explore.worker.mapper.TypeScriptExtendMapper;
import com.ckb.explore.worker.utils.GitUtil;
import com.ckb.explore.worker.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.nervos.ckb.utils.Numeric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class GitPullTokenInfoTask {

    @Value("${ckb.token.git.url:https://github.com/appfi5/ckb-labels.git}")
    private String ckbTokenGitUrl;
    private static final String GIT_REPO_PATH = "/data/udt";


    @Value("${spring.profiles.active}")
    private String active;

    private String UDT_DIR = "/information/udt/" ;

    /** JSON文件后缀 */
    private static final String JSON_SUFFIX = ".json";

    private static  String famousJson = "famous.json";

    @Value("${ckb.token.git.branch:udt-ext}")
    private   String branch ;

    @Resource
    ScriptMapper scriptMapper;

    @Resource
    TypeScriptExtendMapper typeScriptExtendMapper;

//    @Scheduled(cron = "${cron.syncUdtGitCode:0 0 1 * * ?}")
    public void syncUdtGitCode() {
        Git git = null;
        Repository repository = null;
        boolean isFirstInit = !GitUtil.isLocalRepoInited(GIT_REPO_PATH);

        try {

            GitUtil.initLocalEmptyRepo(GIT_REPO_PATH);

            // 1. 初始化 Git 仓库
            git = GitUtil.openLocalRepo(GIT_REPO_PATH);
            if (git == null) {
                log.error("打开本地仓库失败，终止同步");
                return;
            }

             repository = git.getRepository();
            if(isFirstInit){
                RemoteAddCommand remoteAddCommand = git.remoteAdd()
                        .setName("origin")
                        .setUri(new URIish(ckbTokenGitUrl));
                remoteAddCommand.call();
            }
            // 2. 拉取最新代码
            PullResult pullResult = git.pull().setRemote("origin").setRemoteBranchName(branch).call();
            if (!pullResult.isSuccessful()) {
                log.error("Git 拉取失败 {}",pullResult.getFetchResult());
                return;
            }
            if(isFirstInit){
                fullSyncTargetJsonFiles(GIT_REPO_PATH,UDT_DIR+active);
                return;
            }

            if(Objects.equals(pullResult.getMergeResult().getMergeStatus(), MergeResult.MergeStatus.ALREADY_UP_TO_DATE)){
                log.info("Git 无变动 ");
                return;
            }


            // 3. 获取拉取前后的版本差异，得到变更文件列表
            List<DiffEntry> diffEntries = GitUtil.getDiffEntries(repository);
            if (diffEntries.isEmpty()) {
                log.info("暂无文件变更");
                return;
            }

            List<DiffEntry> jsonDiffEntries = GitUtil.filterJsonFiles(UDT_DIR+active,diffEntries);

            // 4. 处理变更文件（新增/更新）
            handleUdtFileChanges(jsonDiffEntries);
            // 关闭资源
            git.close();
            repository.close();

        } catch (IOException | GitAPIException  | URISyntaxException e) {
            if(isFirstInit){
                File localRepoFile = new File(GIT_REPO_PATH);
                if(localRepoFile.exists()){
                    localRepoFile.delete();
                }
            }
            log.error("Git 同步异常：" + e.getMessage());
            e.printStackTrace();
        }
    }





    private void handleUdtFileChanges(List<DiffEntry> jsonDiffEntries) throws IOException {
        for (DiffEntry entry : jsonDiffEntries) {
            String filePath = entry.getNewPath(); // 变更文件的完整路径（相对Git仓库）
            Path absolutePath = Paths.get(GIT_REPO_PATH, filePath); // 本地绝对路径
            String fileName = absolutePath.getFileName().toString(); // 文件名（如test.json）

            String fileId = fileName.substring(0, fileName.lastIndexOf(".")); // 用文件名（不含后缀）作为ID

            // 读取JSON文件内容
            String jsonContent = Files.readString(absolutePath, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(jsonContent)) {
                log.error("文件内容为空：" + filePath);
                continue;
            }
            log.info("entry.getChangeType()：{}" ,entry.getChangeType());
            // 根据变更类型执行操作
            if (entry.getChangeType() == DiffEntry.ChangeType.ADD||entry.getChangeType() == DiffEntry.ChangeType.MODIFY) {

                log.info("JSON文件：" + filePath);
                handleUdtOnGitJson(jsonContent);
            }
        }
    }



    // ========== 首次 Pull 后：全量处理目标 JSON 文件 ==========
    private void fullSyncTargetJsonFiles(String localPath,String jsonPath ) throws IOException {
        File targetDir = new File(localPath  + jsonPath);

        // 目标路径不存在则直接返回
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            log.info("目标 JSON 路径不存在：" + targetDir.getAbsolutePath());
            return;
        }

        // 递归遍历所有 JSON 文件（全量）
        List<File> allJsonFiles = GitUtil.listAllTargetJsonFiles(targetDir);
        if (allJsonFiles.isEmpty()) {
            log.info("目标路径下无 JSON 文件可同步");
            return;
        }

        // 全量处理（示例：打印文件信息，可替换为入库/业务逻辑）
        for (File jsonFile : allJsonFiles) {

            String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
            log.info("新增JSON文件并入库：" + jsonFile.getAbsolutePath());
            handleUdtOnGitJson(jsonContent);
        }
    }


   /*
    eg . {
  "$schema": "../../schema.json",
  "name": "R-ordi",
  "symbol": "R-ordi",
  "icon": null,
  "decimal": 18,
  "manager": "ckb1qpr9n80d4pwspvwyje8qwg6mlgyl7fwfmrcux55z4gy9plqugkzj7qtkxuet29htrf3e3vgrunx25y67sm00tln0wfjxjqgwj6j",
  "type": {
    "codeHash": "0x50bd8d6680b8b9cf98b73f3c08faf8b2a21914311954118ad6609be6e78a1b95",
    "hashType": "data1",
    "args": "0xf283825c247337e4c6048a24daa688570e0055ed18fa026ab169fe42e4b59e4c"
  },
  "typeHash": "0x00286029b4a75e96d82f5e8247dc44056b26d625ecc5a956381c7ef7c1dd9a1e",
  "description": null,
  "udtType": "xudt",
  "published": true,
  "email": null,
  "famous": false,
  "operatorWebsite": null
}
    */
    private  void handleUdtOnGitJson(String json){
        try {
             Map<String,Object> map = JsonUtil.parseObject(json,Map.class);
             if(Objects.equals(true,map.get("famous"))){
                 log.info("famous is {}",map.get("typeHash"));
                 Script script = scriptMapper.findByScriptHashScript(Numeric.hexStringToByteArray(map.get("typeHash").toString()));
                 if(script==null){
                     return;
                 }
//                 TypeScriptExtend typeScriptExtend = new TypeScriptExtend();
//                 typeScriptExtend.setScriptId(script.getId());
//                 typeScriptExtend.setUdtHash(script.getScriptHash());
//                 typeScriptExtend.setSymbol(ObjectToStr(map.get("symbol")));
//                 typeScriptExtend.setName(ObjectToStr(map.get("name")));
//                 typeScriptExtend.setDecimal(Integer.parseInt(ObjectToStr(map.get("decimal"))));
//                 typeScriptExtend.setDescription(ObjectToStr(map.get("description")));
//                 typeScriptExtend.setIconFile(ObjectToStr(map.get("icon")));
//                 typeScriptExtend.setIssuerAddress(ObjectToStr(map.get("manager")));
//                 typeScriptExtend.setOperatorWebsite(ObjectToStr(map.get("operatorWebsite")));
//                 typeScriptExtend.setCreatedAt(script.getTimestamp());
//                 typeScriptExtendMapper.insert(typeScriptExtend);
             }
        }catch (Exception e){

        }

    }

    private  String ObjectToStr(Object obj){
        if(obj==null){
            return "";
        }else {
            return obj.toString();
        }

    }


}

