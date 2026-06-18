package com.rtsbuilding.rtsbuilding.server.pipeline.mining;

import com.rtsbuilding.rtsbuilding.server.pipeline.context.MiningContext;
import com.rtsbuilding.rtsbuilding.server.pipeline.core.PipelineContext;
import com.rtsbuilding.rtsbuilding.server.pipeline.core.TickResult;
import com.rtsbuilding.rtsbuilding.server.pipeline.core.TickablePipe;
import com.rtsbuilding.rtsbuilding.server.pipeline.validation.SessionValidatePipe;
import com.rtsbuilding.rtsbuilding.server.service.mining.RtsMiningStateMachine;
import com.rtsbuilding.rtsbuilding.server.storage.session.RtsStorageSession;

/**
 * Tickable pipe that monitors ultimine/area-mine/area-destroy batch completion
 * across multiple server ticks.
 *
 * <p><b>进度汇报职责已移交：</b>
 * 实际的进度汇报（{@code token.updateProgress()}）改由
 * {@code processUltimineTargets()} 在每个 tick 的批量处理中直接完成。
 * 本 pipe 仅负责：</p>
 * <ol>
 *   <li>检测挖掘是否仍在进行中，若仍在进行则返回 {@link TickResult#running()}。</li>
 *   <li>检测排队模式的等待状态，防止将属于前一个 pipeline 的进度错误记入。</li>
 *   <li>挖掘完毕后返回 {@link TickResult#done()}，触发
 *       {@link com.rtsbuilding.rtsbuilding.server.pipeline.core.ActivePipeline#completeWorkflow()}
 *       安全网关闭工作流条目。</li>
 * </ol>
 *
 * <p><b>Preconditions:</b> The pipeline context must contain a resolved session
 * ({@link SessionValidatePipe#KEY_SESSION}) and a workflow entry ID
 * ({@link PipelineContext#KEY_WORKFLOW_ENTRY_ID}).</p>
 */
public final class UltimineTickPipe implements TickablePipe {

    @Override
    public TickResult tick(PipelineContext ctx) {
        MiningContext mctx = MiningContext.require(ctx);
        RtsStorageSession session = mctx.getResolvedSession();
        if (session == null) {
            return TickResult.error("No session in context");
        }

        // ── Check if mining is still active ──────────────────────────────
        boolean miningActive = session.mining.miningPos != null
                || !session.mining.ultimineTargets.isEmpty()
                || session.mining.ultimineProgressPos != null
                || !session.mining.ultimineJobQueue.isEmpty();

        if (miningActive) {
            // ── Queue mode detection ────────────────────────────────────
            //    Pipeline 2 is registered while Pipeline 1 is still running.
            //    If our entry ID is not the one currently tracked by
            //    RtsMiningStateMachine.WORKFLOW_ENTRY_IDS, we are waiting
            //    in queue — just return running without any action.
            boolean inQueueWait = !mctx.hasWorkflowEntryId()
                    || RtsMiningStateMachine.getWorkflowEntryId(mctx.player().getUUID()) != mctx.getWorkflowEntryId();
            if (inQueueWait) {
                return TickResult.running();
            }

            // Mining is active — progress is reported directly by
            // processUltimineTargets() in the tickActiveMining() call.
            return TickResult.running();
        }

        // ── Mining is done — return done to trigger safety-net cleanup. ──
        //    In the normal survival path the business logic
        //    (finishUltimineBatch → finalizeMiningOperation) already
        //    completed the entry via WorkflowCompletePipe before this pipe
        //    detects done() — since token.complete() is idempotent, the
        //    safety-net call in ActivePipeline.completeWorkflow() is harmless.
        //    In edge cases (creative mode, empty targets) the safety net is
        //    the ONLY completion call, preventing a dangling workflow entry.
        return TickResult.done();
    }
}
