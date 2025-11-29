package com.rivers.core.task;

import com.rivers.core.entity.JobParamReq;
import com.rivers.core.util.SpringContextUtil;
import com.rivers.core.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("job")
@Slf4j
public class BatchController {

    @PostMapping("execute")
    public ResultVO<Void> execute(@RequestBody JobParamReq jobParamReq) {
        log.info("开始执行批处理任务: {}, 类型: {}", jobParamReq.getTaskName(), jobParamReq.getParams());
        BatchTaskHandler bean = SpringContextUtil.getBean(jobParamReq.getTaskName(), BatchTaskHandler.class);
        CompletableFuture.runAsync(() -> bean.execute(jobParamReq));
        return ResultVO.ok();
    }
}
