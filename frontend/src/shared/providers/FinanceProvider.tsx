import React, { createContext, useCallback, useContext, useState } from "react";

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

export const FinanceProvider = ({ children }: { children: React.ReactNode }) => {
  const [budget, setBudgetState] = useState(500000);
  const [budgets, setBudgets] = useState<Record<string, number>>({});
  const [transactions, setTransactions] = useState<Transaction[]>([]);

  const setBudget = useCallback((b: number) => {
    setBudgetState(b);
  }, []);

  const setMonthlyBudget = useCallback((monthKey: string, amount: number) => {
    const normalizedAmount = Number(amount) || 0;

    setBudgets((prev) => {
      const next = {
        ...prev,
        [monthKey]: normalizedAmount,
      };
      return next;
    });

    setBudget(normalizedAmount);
  }, [setBudget]);

  const replaceTransactions = useCallback((items: Transaction[]) => {
    setTransactions(items);
  }, []);

  const addTransaction = useCallback((tx: Transaction) => {
    setTransactions((prev) => [tx, ...prev]);
  }, []);

  const removeTransaction = useCallback((id: string | number) => {
    setTransactions((prev) => prev.filter((tx) => tx.id !== id));
  }, []);

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
