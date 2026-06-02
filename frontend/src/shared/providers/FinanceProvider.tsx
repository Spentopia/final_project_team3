import React, { createContext, useContext, useState, useEffect } from "react";
import { getMe } from "@/domains/auth/api/auth";

export type Transaction = {
  id: string | number;
  date: string;
  amount: number;
  category: string;
  memo?: string;
  type: "expense" | "income";
  receipt?: boolean;
  diary?: string;
};

type FinanceContextType = {
  budget: number;
  setBudget: (b: number) => void;
  budgets: Record<string, number>;
  setMonthlyBudget: (monthKey: string, amount: number) => void;
  transactions: Transaction[];
  replaceTransactions: (items: Transaction[]) => void;
  addTransaction: (t: Transaction) => void;
  removeTransaction: (id: string | number) => void;
};

const FinanceContext = createContext<FinanceContextType | null>(null);

const getFinanceStorageKey = (ownerKey: string, key: string) =>
  `finance:${ownerKey}:${key}`;

export const FinanceProvider = ({ children }: { children: React.ReactNode }) => {
  const [budget, setBudgetState] = useState(500000);
  const [budgets, setBudgets] = useState<Record<string, number>>({});
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [storageOwnerKey, setStorageOwnerKey] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    void getMe()
      .then((me) => {
        if (!cancelled) {
          setStorageOwnerKey(me.id);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setStorageOwnerKey(null);
          setBudgetState(500000);
          setBudgets({});
          setTransactions([]);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!storageOwnerKey) return;

    const savedBudget = localStorage.getItem(getFinanceStorageKey(storageOwnerKey, "budget"));
    const savedBudgets = localStorage.getItem(getFinanceStorageKey(storageOwnerKey, "budgets"));
    const savedTransactions = localStorage.getItem(getFinanceStorageKey(storageOwnerKey, "transactions"));

    if (savedBudget) {
      setBudgetState(Number(savedBudget));
    } else {
      setBudgetState(500000);
    }

    if (savedBudgets) {
      try {
        const parsed = JSON.parse(savedBudgets);
        if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
          setBudgets(parsed);
        }
      } catch {
        localStorage.removeItem(getFinanceStorageKey(storageOwnerKey, "budgets"));
      }
    } else {
      setBudgets({});
    }

    if (savedTransactions) {
      try {
        const parsed = JSON.parse(savedTransactions);
        if (Array.isArray(parsed)) {
          setTransactions(parsed);
        }
      } catch {
        localStorage.removeItem(getFinanceStorageKey(storageOwnerKey, "transactions"));
      }
    } else {
      setTransactions([]);
    }
  }, [storageOwnerKey]);

  useEffect(() => {
    if (!storageOwnerKey) return;
    localStorage.setItem(
      getFinanceStorageKey(storageOwnerKey, "transactions"),
      JSON.stringify(transactions)
    );
  }, [storageOwnerKey, transactions]);

  const setBudget = (b: number) => {
    setBudgetState(b);
    if (storageOwnerKey) {
      localStorage.setItem(getFinanceStorageKey(storageOwnerKey, "budget"), String(b));
    }
  };

  const setMonthlyBudget = (monthKey: string, amount: number) => {
    const normalizedAmount = Number(amount) || 0;

    setBudgets((prev) => {
      const next = {
        ...prev,
        [monthKey]: normalizedAmount,
      };
      if (storageOwnerKey) {
        localStorage.setItem(
          getFinanceStorageKey(storageOwnerKey, "budgets"),
          JSON.stringify(next)
        );
      }
      return next;
    });

    setBudget(normalizedAmount);
  };

  const replaceTransactions = (items: Transaction[]) => {
    setTransactions(items);
  };

  const addTransaction = (tx: Transaction) => {
    setTransactions((prev) => [tx, ...prev]);
  };

  const removeTransaction = (id: string | number) => {
    setTransactions((prev) => prev.filter((tx) => tx.id !== id));
  };

  return (
    <FinanceContext.Provider
      value={{
        budget,
        setBudget,
        budgets,
        setMonthlyBudget,
        transactions,
        replaceTransactions,
        addTransaction,
        removeTransaction,
      }}
    >
      {children}
    </FinanceContext.Provider>
  );
};

export const useFinance = () => {
  const context = useContext(FinanceContext);
  if (!context) throw new Error("FinanceProvider 안에서 사용해야 함");
  return context;
};
