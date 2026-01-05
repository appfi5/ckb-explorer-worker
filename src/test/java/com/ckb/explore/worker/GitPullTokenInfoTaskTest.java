package com.ckb.explore.worker;

import com.ckb.explore.worker.task.GitPullTokenInfoTask;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class GitPullTokenInfoTaskTest {

    @Resource
    GitPullTokenInfoTask tokenInfoTask;

    @Test
    public  void syncUdtGitCode(){
        tokenInfoTask.syncUdtGitCode();
    }

}
