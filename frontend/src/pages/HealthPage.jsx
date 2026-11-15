import React, { useState } from 'react';
import { Activity, RefreshCw, CheckCircle2, AlertTriangle, Server, Shield } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { checkBackendHealth } from '../services/api';

export function HealthPage({ healthStatus, setHealthStatus }) {
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    const result = await checkBackendHealth();
    setHealthStatus(result);
    setRefreshing(false);
  };

  const isUp = healthStatus?.ok;
  const payload = healthStatus?.data;

  return (
    <div className="max-w-5xl mx-auto px-4 py-10 space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight flex items-center space-x-3">
            <Activity className="h-8 w-8 text-blue-400" />
            <span>Backend Diagnostic Monitor</span>
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time telemetry probing the Spring Boot <code>/api/v1/health</code> endpoint.
          </p>
        </div>
        <Button
          onClick={handleRefresh}
          disabled={refreshing}
          variant="secondary"
          className="flex items-center space-x-2"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
          <span>{refreshing ? 'Probing...' : 'Re-check Health'}</span>
        </Button>
      </div>

      {/* Main Status Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className={`border-2 ${isUp ? 'border-emerald-500/40 bg-emerald-950/10' : 'border-amber-500/40 bg-amber-950/10'}`}>
          <CardHeader>
            <CardDescription>Service State</CardDescription>
            <CardTitle className="text-2xl flex items-center space-x-2">
              {isUp ? (
                <>
                  <CheckCircle2 className="h-6 w-6 text-emerald-400" />
                  <span className="text-emerald-400">ONLINE</span>
                </>
              ) : (
                <>
                  <AlertTriangle className="h-6 w-6 text-amber-400" />
                  <span className="text-amber-400">STANDALONE</span>
                </>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-slate-400">
            {isUp
              ? "HTTP 200 OK received from Spring Boot."
              : "Backend server not detected on localhost:8080."}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Target Architecture</CardDescription>
            <CardTitle className="text-xl text-white flex items-center space-x-2">
              <Server className="h-5 w-5 text-blue-400" />
              <span>Modular Monolith</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-slate-400">
            9 High-cohesion domain modules running in a single Java 21 process.
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Security Layer</CardDescription>
            <CardTitle className="text-xl text-white flex items-center space-x-2">
              <Shield className="h-5 w-5 text-purple-400" />
              <span>Stateless REST</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="text-xs text-slate-400">
            Public health check permitted without authentication; APIs protected by JWT.
          </CardContent>
        </Card>
      </div>

      {/* Live JSON Inspector */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="text-base text-white">Endpoint Telemetry Response</CardTitle>
            <CardDescription>Raw JSON returned by <code>GET /api/v1/health</code></CardDescription>
          </div>
          <Badge variant={isUp ? "success" : "outline"}>
            {isUp ? "Live Payload" : "Cached / Offline"}
          </Badge>
        </CardHeader>
        <CardContent>
          <pre className="p-4 rounded-lg bg-slate-950 border border-slate-800 text-xs font-mono text-emerald-400 overflow-x-auto">
            {payload ? JSON.stringify(payload, null, 2) : JSON.stringify({
              status: "WAITING_FOR_BACKEND",
              message: "Start the backend with: cd backend; .\\mvnw.cmd spring-boot:run",
              targetUrl: "http://localhost:8080/api/v1/health",
              timestamp: new Date().toISOString()
            }, null, 2)}
          </pre>
        </CardContent>
      </Card>
    </div>
  );
}
