import React from 'react';
import { 
  ShieldCheck, 
  CheckCircle2, 
  Cpu, 
  GitBranch, 
  Database, 
  Zap, 
  ArrowRight, 
  Clock,
  BellRing
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';

export function LandingPage({ setActivePage, healthStatus, loadingHealth }) {
  const isUp = healthStatus?.ok;
  const modules = healthStatus?.data?.modules || {};

  const teamCards = [
    {
      student: "Student 1 (Team Lead)",
      modules: "auth, user, config, common",
      role: "Security & Core Infrastructure",
      description: "Owns stateless JWT authentication, RBAC authorization, user identity, common persistence, and database configuration.",
      badge: "Security Core",
      color: "border-blue-500/40 text-blue-400"
    },
    {
      student: "Student 2",
      modules: "incident, audit",
      role: "Incident Lifecycle & Audit Trails",
      description: "Controls the incident lifecycle, ticket numbering, ownership rules, and persisted audit history.",
      badge: "Lifecycle Engine",
      color: "border-emerald-500/40 text-emerald-400"
    },
    {
      student: "Student 3",
      modules: "assignment, sla",
      role: "Automated Routing & SLA Compliance",
      description: "Implements Strategy Pattern routing (Round-Robin, Workload-Balanced, and Skill-Based), SLA policies, and deadline escalation.",
      badge: "Strategy Pattern",
      color: "border-purple-500/40 text-purple-400"
    },
    {
      student: "Student 4",
      modules: "notification, intelligence, report",
      role: "Observer Alerts & Local Intelligence",
      description: "Implements Observer-based in-app notifications (email delivery is simulated), local similarity suggestions, and reporting.",
      badge: "Observer Pattern",
      color: "border-amber-500/40 text-amber-400"
    }
  ];

  return (
    <div className="space-y-16 py-10">
      {/* Hero Section */}
      <section className="text-center max-w-4xl mx-auto px-4 space-y-6">
        <div className="inline-flex items-center space-x-2 bg-blue-950/60 border border-blue-800/50 px-3 py-1 rounded-full text-xs text-blue-300">
          <Zap className="h-3.5 w-3.5 text-blue-400" />
          <span>College Java OOP Collaborative Project Foundation Ready</span>
        </div>

        <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-white tracking-tight leading-tight">
          Intelligent IT Service Management <br />
          <span className="bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-400 bg-clip-text text-transparent">
            & Incident Resolution System
          </span>
        </h1>

          <p className="text-base sm:text-lg text-slate-300 max-w-2xl mx-auto leading-relaxed">
          A service operations workspace for reporting incidents, coordinating response teams, and tracking service level commitments across your organization.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-4 pt-2">
          <Button size="lg" onClick={() => setActivePage('health')} className="flex items-center space-x-2">
            <span>Inspect System Health</span>
            <ArrowRight className="h-4 w-4" />
          </Button>
          <Button size="lg" variant="outline" onClick={() => setActivePage('incidents')}>
            Explore Incident Flow
          </Button>
        </div>
      </section>

      {/* Backend Live Connectivity Status Banner */}
      <section className="max-w-5xl mx-auto px-4">
        <Card className={`border-2 ${isUp ? 'border-emerald-500/40 bg-emerald-950/10' : 'border-amber-500/40 bg-amber-950/10'}`}>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div className="flex items-center space-x-3">
              {isUp ? (
                <div className="p-2 rounded-lg bg-emerald-500/20 text-emerald-400">
                  <CheckCircle2 className="h-6 w-6" />
                </div>
              ) : (
                <div className="p-2 rounded-lg bg-amber-500/20 text-amber-400">
                  <Clock className="h-6 w-6" />
                </div>
              )}
              <div>
                <CardTitle className="text-lg">
                  {isUp ? "Backend Service Live & Connected" : "Backend Health Endpoint Ready"}
                </CardTitle>
                <CardDescription>
                  {isUp
                    ? `Connected to Spring Boot at ${healthStatus?.data?.data?.application || 'I-ITSM'}`
                    : "Start the backend server on port 8080 to establish live communication"}
                </CardDescription>
              </div>
            </div>
              <Badge variant={isUp ? "success" : "warning"}>
              {isUp ? "STATUS: 200 OK" : "BACKEND UNAVAILABLE"}
            </Badge>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2 text-xs font-mono">
              {[
                "auth", "user", "incident", "assignment", "sla",
                "notification", "audit", "intelligence", "report"
              ].map((mod) => (
                <div
                  key={mod}
                  className="p-2 rounded bg-slate-950/70 border border-slate-800 flex items-center justify-between"
                >
                  <span className="text-slate-300 font-semibold">{mod}</span>
                  <span className={isUp ? "text-emerald-400" : "text-slate-400"}>
                    {isUp ? (modules[mod] || "UNKNOWN") : "UNKNOWN"}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </section>

      {/* 4-Student Collaborative Module Ownership Matrix */}
      <section className="max-w-6xl mx-auto px-4 space-y-6">
        <div className="text-center space-y-2">
          <h2 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
            Collaborative Development Ownership
          </h2>
          <p className="text-sm text-slate-400 max-w-xl mx-auto">
            Clean module boundaries enabling four students to code concurrently without merge conflicts.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {teamCards.map((card, idx) => (
            <Card key={idx} className="hover:border-slate-700 transition-colors">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-slate-300">{card.student}</span>
                  <Badge variant="outline" className={card.color}>{card.badge}</Badge>
                </div>
                <CardTitle className="text-base text-white mt-1">{card.role}</CardTitle>
                <CardDescription className="font-mono text-xs text-blue-400">{card.modules}</CardDescription>
              </CardHeader>
              <CardContent className="text-xs text-slate-400 leading-relaxed">
                {card.description}
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      {/* Core Architectural Highlights */}
      <section className="max-w-6xl mx-auto px-4 space-y-6">
        <div className="text-center space-y-2">
          <h2 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
            Demonstrated OOP Design Principles
          </h2>
          <p className="text-sm text-slate-400 max-w-xl mx-auto">
            Practical application of software design patterns and Java 21 features.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card>
            <CardHeader>
              <div className="h-10 w-10 rounded-lg bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 mb-2">
                <GitBranch className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Strategy Pattern (Assignment)</CardTitle>
              <CardDescription>Dynamic Polymorphic Routing</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-slate-400 leading-relaxed">
              Decouples ticket allocation algorithms from controllers. <code>AssignmentStrategy</code> supports Round-Robin, Workload-Balanced, and Skill-Based routing.
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div className="h-10 w-10 rounded-lg bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-400 mb-2">
                <BellRing className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Observer Pattern (Notifications)</CardTitle>
              <CardDescription>Decoupled Event Multicasting</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-slate-400 leading-relaxed">
              Domain state changes multicast to registered <code>IncidentEventObserver</code> instances, isolating simulated email logging and persisted in-app alerts from ticket business logic.
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div className="h-10 w-10 rounded-lg bg-emerald-600/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mb-2">
                <Database className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Encapsulation & Inheritance</CardTitle>
              <CardDescription>Domain-Driven Entity Design</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-slate-400 leading-relaxed">
              Standardized <code>BaseEntity</code> inheritance for timestamps and IDs. Rich domain behaviors (e.g. <code>incident.resolve()</code>) preserve data integrity over raw setters.
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Windows Quickstart Terminal Snippet */}
      <section className="max-w-4xl mx-auto px-4">
        <Card className="bg-slate-950 border-slate-800">
          <CardHeader className="pb-3">
            <div className="flex items-center space-x-2 text-slate-300">
              <Cpu className="h-4 w-4 text-blue-400" />
              <CardTitle className="text-sm font-mono">Windows PowerShell Quick-Start</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <pre className="p-4 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-300 font-mono overflow-x-auto space-y-2">
              <div><span className="text-slate-400"># 1. Run Spring Boot Backend (Java 21 + Maven Wrapper)</span></div>
              <div className="text-emerald-400">cd backend; .\mvnw.cmd spring-boot:run</div>
              <div className="pt-2"><span className="text-slate-400"># 2. Run React Frontend (Vite)</span></div>
              <div className="text-blue-400">cd frontend; npm run dev</div>
              <div className="pt-2"><span className="text-slate-400"># 3. Verify Health Check</span></div>
              <div className="text-purple-400">Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health"</div>
            </pre>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
