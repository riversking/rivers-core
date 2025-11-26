package com.rivers.core.task;

import com.rivers.core.entity.JobParamReq;
import com.rivers.core.util.SpringContextUtil;
import com.rivers.core.vo.ResultVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("job")
public class BatchController {

    private final BusinessTaskHandler businessTaskHandler;

    public BatchController(BusinessTaskHandler businessTaskHandler) {
        this.businessTaskHandler = businessTaskHandler;
    }

    @PostMapping("execute")
    public ResultVO<Void> execute(JobParamReq jobParamReq) {
        BatchTaskHandler bean = SpringContextUtil.getBean(jobParamReq.getServerName(), BatchTaskHandler.class);
        return bean.execute(jobParamReq);
    }
}
