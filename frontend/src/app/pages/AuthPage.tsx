import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { BookOpen, LogIn, UserPlus } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "../auth/AuthContext";

type AuthMode = "login" | "register";

export default function AuthPage({ mode }: { mode: AuthMode }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, register } = useAuth();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isRegister = mode === "register";
  const from = (location.state as { from?: string } | null)?.from ?? "/survey";

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setIsSubmitting(true);

    try {
      if (isRegister) {
        await register({ name, email, password });
        toast.success("Account created");
      } else {
        await login({ email, password });
        toast.success("Signed in");
      }
      navigate(from, { replace: true });
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Authentication failed");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-6 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-blue-600 text-white">
            <BookOpen className="h-6 w-6" />
          </div>
          <h1 className="text-2xl font-semibold text-gray-900">
            {isRegister ? "Create your account" : "Sign in"}
          </h1>
          <p className="mt-2 text-sm text-gray-500">
            Access your literature review workspace
          </p>
        </div>

        <form onSubmit={handleSubmit} className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <div className="space-y-4">
            {isRegister && (
              <div>
                <label htmlFor="name" className="mb-2 block text-sm font-medium text-gray-900">
                  Name
                </label>
                <input
                  id="name"
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  required
                  className="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            )}

            <div>
              <label htmlFor="email" className="mb-2 block text-sm font-medium text-gray-900">
                Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                className="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="password" className="mb-2 block text-sm font-medium text-gray-900">
                Password
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                minLength={6}
                required
                className="w-full rounded-lg border border-gray-300 px-4 py-2.5 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isRegister ? <UserPlus className="h-4 w-4" /> : <LogIn className="h-4 w-4" />}
            {isSubmitting ? "Please wait..." : isRegister ? "Create account" : "Sign in"}
          </button>

          <p className="mt-5 text-center text-sm text-gray-600">
            {isRegister ? "Already have an account?" : "Need an account?"}{" "}
            <Link
              to={isRegister ? "/login" : "/register"}
              className="font-medium text-blue-600 hover:text-blue-700"
            >
              {isRegister ? "Sign in" : "Register"}
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
