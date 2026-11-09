package edu.berkeley.cs186.database.concurrency;

import edu.berkeley.cs186.database.TransactionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LockUtil is a declarative layer which simplifies multigranularity lock
 * acquisition for the user (you, in the last task of Part 2). Generally
 * speaking, you should use LockUtil for lock acquisition instead of calling
 * LockContext methods directly.
 */
public class LockUtil {
    /**
     * Ensure that the current transaction can perform actions requiring
     * `requestType` on `lockContext`.
     *
     * `requestType` is guaranteed to be one of: S, X, NL.
     *
     * This method should promote/escalate/acquire as needed, but should only
     * grant the least permissive set of locks needed. We recommend that you
     * think about what to do in each of the following cases:
     * - The current lock type can effectively substitute the requested type
     * - The current lock type is IX and the requested lock is S
     * - The current lock type is an intent lock
     * - None of the above: In this case, consider what values the explicit
     *   lock type can be, and think about how ancestor looks will need to be
     *   acquired or changed.
     *
     * You may find it useful to create a helper method that ensures you have
     * the appropriate locks on all ancestors.
     */
    public static void ensureSufficientLockHeld(LockContext lockContext, LockType requestType) {
        // requestType must be S, X, or NL
        assert (requestType == LockType.S || requestType == LockType.X || requestType == LockType.NL);

        // Do nothing if the transaction or lockContext is null
        TransactionContext transaction = TransactionContext.getTransaction();
        if (transaction == null || lockContext == null || requestType == LockType.NL) return;

        // You may find these variables useful
        LockContext parentContext = lockContext.parentContext();
        LockType effectiveLockType = lockContext.getEffectiveLockType(transaction);
        LockType explicitLockType = lockContext.getExplicitLockType(transaction);

        // TODO(proj4_part2): implement
        // 1. if the current lock type can effectively substitute the requested type
        // direct return
        if (LockType.substitutable(effectiveLockType, requestType)) {
            return;
        }

        // 2. else try call escalate, if new effective lockType can substitute, direct return
        try {
            lockContext.escalate(transaction);
            LockType lockTypeAfterEscalate = lockContext.getEffectiveLockType(transaction);
            if (LockType.substitutable(lockTypeAfterEscalate, requestType)) {
                return;
            }
        } catch (Exception e) {
            // don't do anything
        }

        // 3. else plan to promote or require lock for current lock, but before that we must
        // make sure all ancestors has proper intent locks by calling ensureAncestors
        ensureAncestors(transaction, lockContext, requestType);

        // 4. at last, we actually call acquire or promote
        LockType explicitLock = lockContext.getExplicitLockType(transaction);

        // check explicit lock, if NL, direct acquire
        // if current explicit lock has IX and request S, we promote to SIX
        // if current explicit lock has IS and request S, promote to S
        // eles situation like from IS/IX/S -> X, promote to X
        if (explicitLock == LockType.NL) {
            lockContext.acquire(transaction, requestType);
        } else if (explicitLock == LockType.IX && requestType == LockType.S) {
            lockContext.promote(transaction, LockType.SIX);
        } else if (explicitLock == LockType.IS && requestType == LockType.S) {
            lockContext.promote(transaction, LockType.S);
        } else {
            lockContext.promote(transaction, LockType.X);
        }
    }

    // TODO(proj4_part2) add any helper methods you want

    /**
     * Before acquiring a lock on a child, ensure—from the top down—that all ancestor nodes hold
     * the appropriate intention locks (an IS lock for read requests, or an IX lock for write requests
     * or SIX).
     * @param transaction transactionContext
     * @param context LockContext
     * @param requestLock request lock type either S or X
     */
    private static void ensureAncestors(TransactionContext transaction,
                                        LockContext context, LockType requestLock) {
        List<LockContext> ancestors = new ArrayList<>();
        LockContext curr = context.parentContext();
        while (curr != null) {
            ancestors.add(curr);
            curr = curr.parentContext();
        }
        // change the order from top to down
        Collections.reverse(ancestors);

        // from top to down, set all ancestors to proper intent lock
        LockType neededIntent = (requestLock == LockType.S) ? LockType.IS : LockType.IX;

        // iterate all locks, if ancestor's effective lock can substitutable for
        // neededIntent lock we can continue to the next ancestor
        // if can not substitute, wo look explicit lock and acquire or promote ot
        // neededIntent lock
        for (LockContext ancestor : ancestors) {
            LockType ancestorLock = ancestor.getEffectiveLockType(transaction);

            // if we want to set to IX to ancestor and current ancestor lockType is S
            // we instead set IX to ancestor we must set IX + S = SIX!
            // bc promote from S to IX is not allowed bc it is not compatible!
            if (neededIntent == LockType.IX && ancestorLock == LockType.S) {
                neededIntent = LockType.SIX;
            }

            if (!LockType.substitutable(ancestorLock, neededIntent)) {
                LockType explicitAncestorLock = ancestor.getExplicitLockType(transaction);
                if (explicitAncestorLock == LockType.NL) {
                    ancestor.acquire(transaction, neededIntent);
                } else {
                    ancestor.promote(transaction, neededIntent);
                }
            }
        }
    }
}
