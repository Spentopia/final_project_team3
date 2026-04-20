import { createContext, useContext, useState, useEffect } from "react";

type Transaction = {
  amount: number;
  category?: string;
};

type FinanceContextType = {
  budget: number;
  setBudget: (b: number) => void;
  transactions: Transaction[];
  addTransaction: (t: Transaction) => void;
};

const FinanceContext = createContext<FinanceContextType | null>(null);

export const FinanceProvider = ({ children }: { children: React.ReactNode }) => {
  // ✅ budget
  const [budget, setBudgetState] = useState(500000);

  // ✅ transactions
  const [transactions, setTransactions] = useState<Transaction[]>(() => {
  const saved = localStorage.getItem("transactions");
  return saved ? JSON.parse(saved) : [];
});

  // 🔥 최초 로딩 시 localStorage에서 불러오기
  useEffect(() => {
    const savedBudget = localStorage.getItem("budget");
    const savedTransactions = localStorage.getItem("transactions");

    if (savedBudget) {
      setBudgetState(Number(savedBudget));
    }

    if (savedTransactions) {
      setTransactions(JSON.parse(savedTransactions));
    }
  }, []);

  // 🔥 budget 저장
  const setBudget = (b: number) => {
    setBudgetState(b);
    localStorage.setItem("budget", String(b));
  };

  // 🔥 transaction 저장
  const addTransaction = (tx: Transaction) => {
  setTransactions((prev) => {
    const updated = [...prev, tx];
    localStorage.setItem("transactions", JSON.stringify(updated));
    return updated;
  });
};

  return (
    <FinanceContext.Provider
      value={{ budget, setBudget, transactions, addTransaction }}
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