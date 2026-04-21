import { createContext, useContext, useState, useEffect } from "react";

export type Transaction = {
  id?: string | number;
  date?: string;
  amount: number;
  category?: string;
  memo?: string;
  type?: "expense" | "income";
  receipt?: boolean;
  diary?: string;
};

type FinanceContextType = {
  budget: number;
  setBudget: (b: number) => void;
  transactions: Transaction[];
  addTransaction: (t: Transaction) => void;
  removeTransaction: (id: string | number) => void;
};

const FinanceContext = createContext<FinanceContextType | null>(null);

const readStoredTransactions = (): Transaction[] => {
  const saved = localStorage.getItem("transactions");
  if (!saved) return [];

  try {
    const parsed = JSON.parse(saved);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

export const FinanceProvider = ({ children }: { children: React.ReactNode }) => {
  const [budget, setBudgetState] = useState(500000);
  const [transactions, setTransactions] = useState<Transaction[]>(readStoredTransactions);

  useEffect(() => {
    const savedBudget = localStorage.getItem("budget");

    if (savedBudget) {
      setBudgetState(Number(savedBudget));
    }
  }, []);

  const setBudget = (b: number) => {
    setBudgetState(b);
    localStorage.setItem("budget", String(b));
  };

  const addTransaction = (tx: Transaction) => {
    setTransactions((prev) => {
      const updated = [tx, ...prev];
      localStorage.setItem("transactions", JSON.stringify(updated));
      return updated;
    });
  };

  const removeTransaction = (id: string | number) => {
    setTransactions((prev) => {
      const updated = prev.filter((tx) => tx.id !== id);
      localStorage.setItem("transactions", JSON.stringify(updated));
      return updated;
    });
  };

  return (
    <FinanceContext.Provider
      value={{ budget, setBudget, transactions, addTransaction, removeTransaction }}
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
