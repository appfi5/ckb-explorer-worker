package com.ckb.explore.worker;

import com.ckb.explore.worker.task.ScriptAnalysisTask;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class ScriptAnalysisTaskTests {

    @Resource
    ScriptAnalysisTask scriptAnalysisTask;

    @Test
    public void sporeTask(){
        scriptAnalysisTask.sporeTask();
    }


    @Test
    public void scriptTypeTask(){
        scriptAnalysisTask.scriptTypeTask();
    }

    @Test
    public void mNftTask(){
        scriptAnalysisTask.mNftTask();
    }
}
