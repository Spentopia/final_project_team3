import { createContext, useContext, useEffect, useState } from "react";

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
  budgets: Record<string, number>;
  setBudget: (monthKey: string, value: number) => void;
  setMonthlyBudget: (monthKey: string, amount: number) => void;
  transactions: Transaction[];
  replaceTransactions: (items: Transaction[]) => void;
  addTransaction: (t: Transaction) => void;
  removeTransaction: (id: string | number) => void;
};

const FinanceContext = createContext<FinanceContextType | null>(null);

export const FinanceProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [transactions, setTransactions] = useState<Transaction[]>(() => {
    const saved = localStorage.getItem("transactions");
    if (!saved) return [];

    try {
      return JSON.parse(saved);
    } catch {
      return [];
    }
  });

  const [budgets, setBudgets] = useState<Record<string, number>>(() => {
    const saved = localStorage.getItem("budgets");
    if (!saved) return {};

    try {
      return JSON.parse(saved);
    } catch {
      return {};
    }
  });

  useEffect(() => {
    localStorage.setItem("transactions", JSON.stringify(transactions));
  }, [transactions]);

  useEffect(() => {
    localStorage.setItem("budgets", JSON.stringify(budgets));
  }, [budgets]);

  const setBudget = (monthKey: string, value: number) => {
    setBudgets((prev) => ({
      ...prev,
      [monthKey]: value,
    }));
  };

  const setMonthlyBudget = (monthKey: string, amount: number) => {
    setBudgets((prev) => ({
      ...prev,
      [monthKey]: amount,
    }));
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
        budgets,
        setBudget,
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
  if (!context) {
    throw new Error("FinanceProvider 안에서 사용해야 함");
  }
  return context;
};