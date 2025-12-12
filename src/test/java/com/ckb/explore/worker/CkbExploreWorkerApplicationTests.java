package com.ckb.explore.worker;

import com.ckb.explore.worker.entity.*;
import com.ckb.explore.worker.mapper.DobExtendMapper;


import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import org.nervos.ckb.utils.Numeric;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
@Slf4j
@RequiredArgsConstructor
class CkbExploreWorkerApplicationTests {



    @Resource
    DobExtendMapper dobExtendMapper;

    @Test
    void contextLoads() {

    }


    @Test
    public void dobExtend(){
        DobExtend dobExtend = new DobExtend();
        dobExtend.setId(0L);
        dobExtend.setName("Unique items");
        dobExtend.setDescription("Only for no cluster spore cell");
        dobExtend.setDobScriptHash(Numeric.hexStringToByteArray("0x2981ed0498836ae970473f56ebf61d8e0eaf2dbe97286d160658d7c2787ce69b"));
        dobExtendMapper.insert(dobExtend);

        dobExtendMapper.flush();
        String tags = "rgb++,layer-1-asset";
        dobExtendMapper.updateTagsById(0L,tags);
    }









}
