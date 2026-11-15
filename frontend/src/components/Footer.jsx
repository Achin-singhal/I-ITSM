import React from 'react';
import { Terminal, Github, BookOpen } from 'lucide-react';

export function Footer() {
  return (
    <footer className="border-t border-slate-800 bg-slate-950 py-10 text-slate-400 text-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 md:grid-cols-4 gap-8">
        <div className="md:col-span-2 space-y-3">
          <div className="flex items-center space-x-2 text-white font-semibold">
            <Terminal className="h-4 w-4 text-blue-400" />
            <span>I-ITSM Platform</span>
          </div>
          <p className="text-xs text-slate-400 max-w-md leading-relaxed">
            Intelligent IT Service Management and Incident Resolution System. Designed as a college Java OOP project demonstrating modular monolithic architecture, Strategy and Observer patterns, and modern REST APIs.
          </p>
          <div className="text-xs text-slate-400">
            Backend: Java 21 + Spring Boot 3.3.5 | Frontend: React + Tailwind CSS | Database: Supabase PostgreSQL
          </div>
        </div>

        <div>
          <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200 mb-3">Team Modules</h4>
          <ul className="space-y-1.5 text-xs">
            <li><span className="text-slate-300 font-medium">Student 1:</span> Auth & User Management</li>
            <li><span className="text-slate-300 font-medium">Student 2:</span> Incident Lifecycle & Audit</li>
            <li><span className="text-slate-300 font-medium">Student 3:</span> Strategy Routing & SLA</li>
            <li><span className="text-slate-300 font-medium">Student 4:</span> Observer Alerts & Local Intelligence</li>
          </ul>
        </div>

        <div>
          <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200 mb-3">Documentation</h4>
          <ul className="space-y-1.5 text-xs text-blue-400">
            <li><span>docs/ARCHITECTURE.md</span></li>
            <li><span>docs/API_CONTRACT.md</span></li>
            <li><span>docs/DATABASE_DESIGN.md</span></li>
            <li><span>README.md Quickstart</span></li>
          </ul>
        </div>
      </div>
    </footer>
  );
}
