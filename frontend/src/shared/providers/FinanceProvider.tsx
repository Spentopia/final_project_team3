import { createContext, useContext, useState, useEffect } from "react";

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
  transactions: Transaction[];
  replaceTransactions: (items: Transaction[]) => void;
  addTransaction: (t: Transaction) => void;
  removeTransaction: (id: string | number) => void;
};

const FinanceContext = createContext<FinanceContextType | null>(null);

export const FinanceProvider = ({ children }: { children: React.ReactNode }) => {
  const [budget, setBudgetState] = useState(500000);
  const [transactions, setTransactions] = useState<Transaction[]>([]);

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
      value={{ budget, setBudget, transactions, replaceTransactions, addTransaction, removeTransaction }}
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
