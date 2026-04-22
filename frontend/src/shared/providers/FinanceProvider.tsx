import { createContext, useContext, useState, useEffect } from "react";

type Transaction = {
  id: number | string; // 🔥 추가
  amount: number;
  category?: string;
  date: string;
};

type FinanceContextType = {
  budgets: Record<string, number>;
  setBudget: (monthKey: string, value: number) => void;
  setMonthlyBudget: (monthKey: string, amount: number) => void;
  transactions: Transaction[];
  addTransaction: (t: Transaction) => void;
  removeTransaction: (id: number | string) => void;
};

const FinanceContext = createContext<FinanceContextType | null>(null);

export const FinanceProvider = ({ children }: { children: React.ReactNode }) => {

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

  // 🔥 transaction 저장
  const addTransaction = (tx: Transaction) => {
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
      {children}
    </FinanceContext.Provider>
  );
};

export const useFinance = () => {
  const context = useContext(FinanceContext);
  if (!context) throw new Error("FinanceProvider 안에서 사용해야 함");
  return context;
};