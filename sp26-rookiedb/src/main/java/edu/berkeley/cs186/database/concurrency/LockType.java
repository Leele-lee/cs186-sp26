package edu.berkeley.cs186.database.concurrency;

/**
 * Utility methods to track the relationships between different lock types.
 */
public enum LockType {
    S,   // shared
    X,   // exclusive
    IS,  // intention shared
    IX,  // intention exclusive
    SIX, // shared intention exclusive
    NL;  // no lock held

    /**
     * This helper method return the index of lockType
     */
    private static int getIndex(LockType type) {
        switch (type) {
            case NL:  return 0;
            case IS:  return 1;
            case IX:  return 2;
            case S:   return 3;
            case SIX: return 4;
            case X:   return 5;
            default: throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    /**
     * Build 2 matrix
     * the order is NL[0], IS[1], IX[2], S[3], SIX[4], X[5]
     */
    private static final boolean[][] COMPATIBILITY_MATRIX = new boolean[][] {
            //         NL     IS     IX    S    SIX    X
            /* NL  */ {true, true, true, true, true, true},
            /* IS  */ {true, true, true, true, true, false},
            /* IX  */ {true, true, true, false, false, false},
            /* S   */ {true, true, false, true, false, false},
            /* SIX */ {true, true, false, false, false, false},
            /* X   */ {true, false, false, false, false, false}
    };

    /**
     * This method checks whether lock types A and B are compatible with
     * each other. If a transaction can hold lock type A on a resource
     * at the same time another transaction holds lock type B on the same
     * resource, the lock types are compatible.
     */
    public static boolean compatible(LockType a, LockType b) {
        if (a == null || b == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement
        return COMPATIBILITY_MATRIX[getIndex(a)][getIndex(b)];
    }

    /**
     * This method returns the lock on the parent resource
     * that should be requested for a lock of type A to be granted.
     */
    public static LockType parentLock(LockType a) {
        if (a == null) {
            throw new NullPointerException("null lock type");
        }
        switch (a) {
        case S: return IS;
        case X: return IX;
        case IS: return IS;
        case IX: return IX;
        case SIX: return IX;
        case NL: return NL;
        default: throw new UnsupportedOperationException("bad lock type");
        }
    }

    /**
     * This method returns if parentLockType has permissions to grant a childLockType
     * on a child.
     */
    public static boolean canBeParentLock(LockType parentLockType, LockType childLockType) {
        if (parentLockType == null || childLockType == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement
        // analyse from childLockType perspective
        // 1. if child is NL, any parentLockType will be okay
        if (childLockType == NL) {
            return true;
        }
        // 2. switch on childLockType
        switch(childLockType) {
            case S:
            case IS:
                // parentLockType must be IS or IX
                return (parentLockType == IS || parentLockType == IX);

            case X:
            case IX:
                // parentLockParent must be IX or SIX
                return (parentLockType == IX || parentLockType == SIX);

            case SIX:
                // parentLockType must be IX or SIX(?)
                return (parentLockType == IX || parentLockType == SIX);

            default:
                return false;
        }
    }

    /**
     *  Build a matrix for substitute
     *  [ X ]  (至高无上：能写全树，能替代一切)
     *            /     \
     *           /       \
     *       [ SIX ]      \
     *       /     \       \
     *      /       \       \
     *    [ S ]    [ IX ]    |
     *      \   \    |       /
     *       \   \   |      /
     *        \   [ IS ]   /
     *         \     |    /
     *          \    |   /
     *             [ NL ]  (最底层：任何锁都能替代 NL)
     *  the order is NL[0], IS[1], IX[2], S[3], SIX[4], X[5]
     *  row represent substitute
     *  column represent required
     */
    private static final boolean[][] SUBSTITUTION_MATRIX = new boolean[][] {
            // sub/req NL     IS     IX    S    SIX    X
            /* NL  */ {true, false, false, false, false, false},
            /* IS  */ {true, true, false, false, false, false},
            /* IX  */ {true, true, true, false, false, false},
            /* S   */ {true, true, false, true, false, false},
            /* SIX */ {true, true, true, true, true, false},
            /* X   */ {true, true, true, true, true, true}
    };

    /**
     * This method returns whether a lock can be used for a situation
     * requiring another lock (e.g. an S lock can be substituted with
     * an X lock, because an X lock allows the transaction to do everything
     * the S lock allowed it to do).
     */
    public static boolean substitutable(LockType substitute, LockType required) {
        if (required == null || substitute == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement
        return SUBSTITUTION_MATRIX[getIndex(substitute)][getIndex(required)];
    }

    /**
     * @return True if this lock is IX, IS, or SIX. False otherwise.
     */
    public boolean isIntent() {
        return this == LockType.IX || this == LockType.IS || this == LockType.SIX;
    }

    @Override
    public String toString() {
        switch (this) {
        case S: return "S";
        case X: return "X";
        case IS: return "IS";
        case IX: return "IX";
        case SIX: return "SIX";
        case NL: return "NL";
        default: throw new UnsupportedOperationException("bad lock type");
        }
    }
}

