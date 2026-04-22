import { createContext, useContext, useState, useEffect } from "react";

<<<<<<< HEAD
type Transaction = {
  id: number | string; // 🔥 추가
  amount: number;
  category?: string;
  date: string;
=======
export type Transaction = {
  id: string | number;
  date: string;
  amount: number;
  category: string;
  memo?: string;
  type: "expense" | "income";
  receipt?: boolean;
  diary?: string;
>>>>>>> develop
};

type FinanceContextType = {
  budgets: Record<string, number>;
  setBudget: (monthKey: string, value: number) => void;
  setMonthlyBudget: (monthKey: string, amount: number) => void;
  transactions: Transaction[];
  replaceTransactions: (items: Transaction[]) => void;
  addTransaction: (t: Transaction) => void;
<<<<<<< HEAD
  removeTransaction: (id: number | string) => void;
=======
  removeTransaction: (id: string | number) => void;
>>>>>>> develop
};

const FinanceContext = createContext<FinanceContextType | null>(null);

export const FinanceProvider = ({ children }: { children: React.ReactNode }) => {
<<<<<<< HEAD

  // ✅ transactions
  const [transactions, setTransactions] = useState<Transaction[]>(() => {
  const saved = localStorage.getItem("transactions");
  return saved ? JSON.parse(saved) : [];
});

// ✅ 월별 예산 상태 추가
const [budgets, setBudgets] = useState<Record<string, number>>(() => {
  const saved = localStorage.getItem("budgets");
  return saved ? JSON.parse(saved) : {};
});

// ✅ 월별 예산 설정 함수
const setMonthlyBudget = (monthKey: string, amount: number) => {
  const updated = {
    ...budgets,
    [monthKey]: amount,
  };

  setBudgets(updated);
  localStorage.setItem("budgets", JSON.stringify(updated));
};

  // 🔥 최초 로딩 시 localStorage에서 불러오기
  useEffect(() => {
    const savedBudgets = localStorage.getItem("budgets");
    const savedTransactions = localStorage.getItem("transactions");

    if (savedBudgets) {
  setBudgets(JSON.parse(savedBudgets));
}

    if (savedTransactions) {
      setTransactions(JSON.parse(savedTransactions));
    }
  }, []);

  // 🔥 budget 저장
  const setBudget = (monthKey: string, value: number) => {
  setBudgets((prev) => {
    const updated = {
      ...prev,
      [monthKey]: value,
    };

    localStorage.setItem("budgets", JSON.stringify(updated));
    return updated;
  });
};
=======
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
>>>>>>> develop

  const replaceTransactions = (items: Transaction[]) => {
    setTransactions(items);
  };

  const addTransaction = (tx: Transaction) => {
<<<<<<< HEAD
  setTransactions((prev) => {
    const updated = [...prev, tx];
    localStorage.setItem("transactions", JSON.stringify(updated));
    return updated;
  });
};
const removeTransaction = (id: number | string) => {
  setTransactions((prev) => {
    const updated = prev.filter((t) => t.id !== id);

    localStorage.setItem("transactions", JSON.stringify(updated));

    return updated;
  });
};

  return (
    <FinanceContext.Provider
  value={{
  setBudget,
  transactions,
  addTransaction,
  removeTransaction,
  budgets,
  setMonthlyBudget
}}
>
=======
    setTransactions((prev) => [tx, ...prev]);
  };

  const removeTransaction = (id: string | number) => {
    setTransactions((prev) => prev.filter((tx) => tx.id !== id));
  };

  return (
    <FinanceContext.Provider
      value={{ budget, setBudget, transactions, replaceTransactions, addTransaction, removeTransaction }}
    >
>>>>>>> develop
      {children}
    </FinanceContext.Provider>
  );
};

export const useFinance = () => {
  const context = useContext(FinanceContext);
  if (!context) throw new Error("FinanceProvider 안에서 사용해야 함");
  return context;
};
