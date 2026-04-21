alter table public.expenses
add column if not exists transaction_type text not null default 'expense';

update public.expenses
set transaction_type = 'expense'
where transaction_type is null;

alter table public.expenses
add constraint expenses_transaction_type_check
check (transaction_type in ('expense', 'income'));

create index if not exists idx_expenses_user_id_transaction_type_expense_date
on public.expenses (user_id, transaction_type, expense_date desc);
