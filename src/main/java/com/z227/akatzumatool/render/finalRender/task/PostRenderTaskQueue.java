package com.z227.akatzumatool.render.finalRender.task;

import com.z227.akatzumatool.render.finalRender.PostRenderPhase;

// PostRenderTaskQueue 统一无实体任务队列的提交、状态判断、渲染和清理入口。
public interface PostRenderTaskQueue<T extends PostRenderTask> {
    // 返回该队列接收的任务类型。
    public PostRenderQueueType queueType();

    // 返回该队列所属后处理阶段。
    public PostRenderPhase phase();

    // 添加一个任务到具体队列。
    public void add(T task);

    // 判断该队列是否存在当前帧或跨帧活跃内容。
    public boolean hasActive();

    // 在后处理阶段渲染该队列。
    public void render(PostRenderTaskRenderContext context);

    // 清空该队列的待处理和活跃内容。
    public void clear();
}
