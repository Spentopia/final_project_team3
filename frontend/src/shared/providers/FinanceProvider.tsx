import React, { createContext, useContext, useState, useEffect } from "react";

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

  useEffect(() => {
    const savedBudget = localStorage.getItem("budget");
    const savedBudgets = localStorage.getItem("budgets");
    const savedTransactions = localStorage.getItem("transactions");

    if (savedBudget) {
      setBudgetState(Number(savedBudget));
    }

    if (savedBudgets) {
      try {
        const parsed = JSON.parse(savedBudgets);
        if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
          setBudgets(parsed);
        }
      } catch {
        localStorage.removeItem("budgets");
      }
    }

    if (savedTransactions) {
      try {
        const parsed = JSON.parse(savedTransactions);
        if (Array.isArray(parsed)) {
          setTransactions(parsed);
        }
      } catch {
        localStorage.removeItem("transactions");
      }
    }
  }, []);

  useEffect(() => {
    localStorage.setItem("transactions", JSON.stringify(transactions));
  }, [transactions]);

  const setBudget = (b: number) => {
    setBudgetState(b);
    localStorage.setItem("budget", String(b));
  };

  const setMonthlyBudget = (monthKey: string, amount: number) => {
    const normalizedAmount = Number(amount) || 0;

    setBudgets((prev) => {
      const next = {
        ...prev,
        [monthKey]: normalizedAmount,
      };
      localStorage.setItem("budgets", JSON.stringify(next));
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
