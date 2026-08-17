package com.z227.akatzumatool.render.finalRender.task;

// PostRenderTask 是无实体后处理任务的统一提交接口。
public interface PostRenderTask {
    // 返回任务所属队列类型，FinalRender 根据该类型分发到注册队列。
    public PostRenderQueueType queueType();
}
