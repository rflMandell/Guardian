class StorageAdapter {
  private memory = new Map<string, string>();

  async getItem(key: string): Promise<string | null> {
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem(key);
    }
    return this.memory.get(key) || null;
  }

  async setItem(key: string, value: string): Promise<void> {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(key, value);
    } else {
      this.memory.set(key, value);
    }
  }

  async removeItem(key: string): Promise<void> {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(key);
    } else {
      this.memory.delete(key);
    }
  }
}

export const appStorage = new StorageAdapter();
